using System.Collections.Concurrent;
using System.Globalization;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Security;

namespace CartaoSus.Api.Services;

// ============================================================
// Acolhimento (E3): handshake NFC + TOTP com bloqueio (SDD 5.4 e 5.3)
// ============================================================
public interface IAcolhimentoService
{
    Task<ValidarNfcResponse> ValidarNfcAsync(ValidarNfcRequest req, ClaimsPrincipal terminal);
}

public class AcolhimentoService(
    ICidadaoRepository cidadaos,
    IUnidadeRepository unidades,
    ISessaoRepository sessoes,
    IAlertaRepository alertas,
    IDocumentoRepository documentos,
    IHistoricoRepository historicos,
    ISinalVitalRepository sinais,
    AntiReusoTotp antiReuso) : IAcolhimentoService
{
    private static readonly TimeSpan Janela = TimeSpan.FromMinutes(10);
    private const int LimiteFalhas = 3;

    public async Task<ValidarNfcResponse> ValidarNfcAsync(ValidarNfcRequest req, ClaimsPrincipal terminal)
    {
        if (terminal.Papel() != "DISPOSITIVO_TERMINAL_NFC")
            throw new SemPermissaoException("Acolhimento só pode ser iniciado por terminal NFC.");

        var unidade = await unidades.PorCnesAsync(req.CodigoCnesUnidade)
            ?? throw new NaoEncontradoException("Unidade não encontrada");
        if (terminal.IdUnidade() != unidade.IdUnidade)
            throw new SemPermissaoException("Terminal não pertence a esta unidade.");

        var cidadao = await cidadaos.PorCnsAsync(req.NumeroCns)
            ?? throw new NaoEncontradoException("CNS não encontrado");

        var desde = DateTime.Now - Janela;
        var rejeicoes = await sessoes.ContarRejeicoesDesdeAsync(cidadao.IdCidadao, desde);

        if (rejeicoes >= LimiteFalhas)
        {
            await RegistrarAsync(cidadao.IdCidadao, unidade.IdUnidade, req.TokenRotativo, "REJEITADO", "Bloqueado");
            throw new BloqueadoException();
        }

        var contador = TotpService.ValidarJanela(cidadao.SegredoTotp, req.TokenRotativo, DateTimeOffset.UtcNow);
        if (contador is null || !antiReuso.Registrar(cidadao.IdCidadao, contador.Value))
        {
            var motivo = contador is null ? "Token inválido" : "Token já utilizado";
            await RegistrarAsync(cidadao.IdCidadao, unidade.IdUnidade, req.TokenRotativo, "REJEITADO", motivo);

            // Ao atingir a 3ª falha dentro da janela, registra o alerta de segurança (SDD 9.4).
            if (rejeicoes + 1 == LimiteFalhas)
            {
                await alertas.AdicionarAsync(new Alerta
                {
                    IdCidadao = cidadao.IdCidadao,
                    TipoAlerta = "TENTATIVA_INDEVIDA",
                    Severidade = "ALTA",
                    Mensagem = $"{LimiteFalhas} tentativas inválidas de leitura detectadas na unidade {unidade.NomeEstabelecimento}",
                    StatusEnvio = "PENDENTE",
                    CriadoEm = DateTime.Now
                });
                await alertas.SalvarAsync();
            }
            throw new TokenInvalidoException();
        }

        await RegistrarAsync(cidadao.IdCidadao, unidade.IdUnidade, req.TokenRotativo, "AUTORIZADO", null);
        return await MontarResumoAsync(cidadao);
    }

    private async Task RegistrarAsync(long idCidadao, long idUnidade, string token, string status, string? motivo)
    {
        await sessoes.AdicionarAsync(new SessaoAcesso
        {
            IdCidadao = idCidadao,
            IdUnidade = idUnidade,
            TokenUtilizado = token,
            StatusAutenticacao = status,
            Motivo = motivo,
            DataHoraAcesso = DateTime.Now
        });
        await sessoes.SalvarAsync();
    }

    private async Task<ValidarNfcResponse> MontarResumoAsync(Cidadao c)
    {
        var docs = (await documentos.DoCidadaoAsync(c.IdCidadao)).Select(d => d.TipoDocumento).Distinct().ToList();
        var hist = await historicos.DoCidadaoAsync(c.IdCidadao);
        var criticos = hist.Where(h => h.TipoRegistro == "ALERGIA" && h.GrauSeveridade == "CRITICO")
            .Select(h => new AlertaCritico("ALERGIA", h.Descricao, "CRITICO")).ToList();

        var medicoes = await sinais.ListarAsync(c.IdCidadao, null, null, null);
        var glicemia = medicoes.LastOrDefault(s => s.TipoMedicao == "GLICEMIA");
        var sist = medicoes.LastOrDefault(s => s.TipoMedicao == "PRESSAO_SISTOLICA");
        var diast = medicoes.LastOrDefault(s => s.TipoMedicao == "PRESSAO_DIASTOLICA");
        string? pressao = sist is not null && diast is not null
            ? $"{sist.ValorMedido:0}/{diast.ValorMedido:0}" : null;
        var data = new[] { glicemia?.DataHora, sist?.DataHora, diast?.DataHora }
            .Where(d => d is not null).Select(d => d!.Value).DefaultIfEmpty().Max();

        var paciente = new PacienteResumo(
            c.NomeCompleto, c.NumeroCns, docs, criticos,
            new UltimasMedicoes(glicemia?.ValorMedido, pressao, data == default ? null : data));

        return new ValidarNfcResponse("AUTORIZADO",
            "Identificação e autorização validadas com sucesso.", paciente);
    }
}

// ============================================================
// Telemetria (E6) e alertas (E10): limiares e origem IOT/APP (SDD 9.4)
// ============================================================
public interface ITelemetriaService
{
    Task<SinalVitalResposta> IngerirAsync(SinalVitalRequest req, ClaimsPrincipal user);
}

public class TelemetriaService(
    ICidadaoRepository cidadaos,
    ISinalVitalRepository sinais,
    IAlertaRepository alertas) : ITelemetriaService
{
    private static readonly HashSet<string> TiposValidos =
        ["PRESSAO_SISTOLICA", "PRESSAO_DIASTOLICA", "GLICEMIA", "PESO", "FREQUENCIA_CARDIACA"];

    public async Task<SinalVitalResposta> IngerirAsync(SinalVitalRequest req, ClaimsPrincipal user)
    {
        if (!TiposValidos.Contains(req.TipoMedicao))
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["tipoMedicao"] = "tipo de medição inválido" });
        if (req.Valor is null || req.Valor <= 0)
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["valor"] = "deve ser maior que zero" });

        long idCidadao;
        string origem;
        if (user.Papel() == "DISPOSITIVO_TENSIOMETRO")
        {
            idCidadao = user.IdCidadao() ?? throw new SemPermissaoException("Tensiômetro sem vínculo.");
            if (req.NumeroCns is not null)
            {
                var informado = await cidadaos.PorCnsAsync(req.NumeroCns);
                if (informado is null || informado.IdCidadao != idCidadao)
                    throw new SemPermissaoException("CNS não corresponde ao cidadão vinculado ao dispositivo.");
            }
            origem = "IOT";
        }
        else if (user.EhCidadao())
        {
            idCidadao = user.IdCidadao() ?? throw new SemPermissaoException("Usuário sem cadastro de cidadão.");
            origem = "APP";
        }
        else
        {
            throw new SemPermissaoException("Este perfil não pode enviar telemetria.");
        }

        var sinal = new SinalVital
        {
            IdCidadao = idCidadao,
            TipoMedicao = req.TipoMedicao,
            ValorMedido = req.Valor.Value,
            Origem = origem,
            DataHora = req.DataHora ?? DateTime.Now
        };
        await sinais.AdicionarAsync(sinal);

        AlertaDisparado? alerta = null;
        if ((req.TipoMedicao == "PRESSAO_SISTOLICA" && req.Valor > 180)
            || (req.TipoMedicao == "PRESSAO_DIASTOLICA" && req.Valor > 110))
        {
            alerta = new AlertaDisparado("PRESSAO_CRITICA", "Alerta de Pressão Crítica: Procure a UPA mais próxima");
            await CriarAlertaAsync(idCidadao, "PRESSAO_CRITICA", alerta.Mensagem);
        }
        else if (req.TipoMedicao == "GLICEMIA" && req.Valor >= 300)
        {
            alerta = new AlertaDisparado("GLICEMIA_CRITICA", "Glicemia crítica: procure atendimento de saúde");
            await CriarAlertaAsync(idCidadao, "GLICEMIA_CRITICA", alerta.Mensagem);
        }

        await sinais.SalvarAsync();
        return new SinalVitalResposta(sinal.IdSinal, alerta is not null, alerta);
    }

    private async Task CriarAlertaAsync(long idCidadao, string tipo, string mensagem)
    {
        await alertas.AdicionarAsync(new Alerta
        {
            IdCidadao = idCidadao,
            TipoAlerta = tipo,
            Severidade = "CRITICA",
            Mensagem = mensagem,
            StatusEnvio = "PENDENTE",
            CriadoEm = DateTime.Now
        });
        await alertas.SalvarAsync();
    }
}

// ============================================================
// Cidadão: prontuário, sinais, histórico, alertas e acessos (E4, E5, E7, E10)
// Regra de acesso: o próprio cidadão, ou um profissional com acolhimento AUTORIZADO nos últimos 10 min.
// ============================================================
public interface ICidadaoService
{
    Task<ProntuarioResposta> ProntuarioAsync(long id, ClaimsPrincipal user);
    Task<SinaisVitaisResposta> SinaisAsync(long id, string? tipo, DateOnly? de, DateOnly? ate, ClaimsPrincipal user);
    Task<HistoricoResposta> AtualizarHistoricoAsync(long id, long idHistorico, AtualizarHistoricoRequest req, ClaimsPrincipal user);
    Task<PaginaResposta<AlertaResumo>> AlertasAsync(long id, string? status, int page, int size, ClaimsPrincipal user);
    Task MarcarAlertaEntregueAsync(long id, long idAlerta, ClaimsPrincipal user);
    Task<IReadOnlyList<AcessoResumo>> AcessosAsync(long id, ClaimsPrincipal user);
}

public class CidadaoService(
    ICidadaoRepository cidadaos,
    IHistoricoRepository historicos,
    ISinalVitalRepository sinais,
    IAlertaRepository alertas,
    ISessaoRepository sessoes) : ICidadaoService
{
    private static readonly TimeSpan JanelaConsentimento = TimeSpan.FromMinutes(10);
    private static readonly HashSet<string> GrausValidos = ["LEVE", "MODERADO", "ALTO", "CRITICO"];

    public async Task<ProntuarioResposta> ProntuarioAsync(long id, ClaimsPrincipal user)
    {
        var cidadao = await CarregarComAcessoAsync(id, user);
        var hist = await historicos.DoCidadaoAsync(id);
        var pend = await alertas.PendentesAsync(id);
        return new ProntuarioResposta(cidadao.IdCidadao, cidadao.NomeCompleto, cidadao.NumeroCns,
            hist.Select(h => new ProntuarioHistorico(h.TipoRegistro, h.Descricao, h.GrauSeveridade, h.DataRegistro)).ToList(),
            pend.Select(ToResumo).ToList());
    }

    public async Task<SinaisVitaisResposta> SinaisAsync(long id, string? tipo, DateOnly? de, DateOnly? ate, ClaimsPrincipal user)
    {
        await CarregarComAcessoAsync(id, user);
        var inicio = de?.ToDateTime(TimeOnly.MinValue);
        var fim = ate?.AddDays(1).ToDateTime(TimeOnly.MinValue);
        var lista = await sinais.ListarAsync(id, tipo, inicio, fim);
        var valores = lista.Select(s => s.ValorMedido).ToList();
        var resumo = new ResumoMedicoes(
            valores.Count == 0 ? null : Math.Round(valores.Average(), 2),
            valores.Count == 0 ? null : valores.Min(),
            valores.Count == 0 ? null : valores.Max(),
            valores.Count);
        return new SinaisVitaisResposta(tipo,
            lista.Select(s => new MedicaoResumo(s.ValorMedido, s.Origem, s.DataHora)).ToList(), resumo);
    }

    public async Task<HistoricoResposta> AtualizarHistoricoAsync(long id, long idHistorico, AtualizarHistoricoRequest req, ClaimsPrincipal user)
    {
        if (!user.EhCidadao() || user.IdCidadao() != id)
            throw new SemPermissaoException("Apenas o próprio cidadão pode alterar seu histórico.");
        if (!GrausValidos.Contains(req.GrauSeveridade))
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["grauSeveridade"] = "deve ser LEVE, MODERADO, ALTO ou CRITICO" });

        var h = await historicos.PorIdAsync(id, idHistorico)
            ?? throw new NaoEncontradoException("Registro clínico não encontrado.");
        h.Descricao = req.Descricao;
        h.GrauSeveridade = req.GrauSeveridade;
        await historicos.SalvarAsync();
        return new HistoricoResposta(h.IdHistorico, h.TipoRegistro, h.Descricao, h.GrauSeveridade, h.DataRegistro);
    }

    public async Task<PaginaResposta<AlertaResumo>> AlertasAsync(long id, string? status, int page, int size, ClaimsPrincipal user)
    {
        await CarregarComAcessoAsync(id, user);
        var (itens, total) = await alertas.ListarAsync(id, status, page, size);
        return new PaginaResposta<AlertaResumo>(itens.Select(ToResumo).ToList(), page, size, total);
    }

    public async Task MarcarAlertaEntregueAsync(long id, long idAlerta, ClaimsPrincipal user)
    {
        if (!user.EhCidadao() || user.IdCidadao() != id)
            throw new SemPermissaoException("Apenas o próprio cidadão pode confirmar alertas.");
        var a = await alertas.PorIdAsync(id, idAlerta)
            ?? throw new NaoEncontradoException("Alerta não encontrado.");
        a.StatusEnvio = "ENTREGUE";
        await alertas.SalvarAsync();
    }

    public async Task<IReadOnlyList<AcessoResumo>> AcessosAsync(long id, ClaimsPrincipal user)
    {
        await CarregarComAcessoAsync(id, user);
        var ult = await sessoes.UltimasAsync(id, 20);
        return ult.Select(s => new AcessoResumo(s.IdSessao, s.StatusAutenticacao, s.Motivo, s.DataHoraAcesso, s.IdUnidade)).ToList();
    }

    private async Task<Cidadao> CarregarComAcessoAsync(long id, ClaimsPrincipal user)
    {
        var cidadao = await cidadaos.PorIdAsync(id) ?? throw new NaoEncontradoException("Cidadão não encontrado.");
        if (user.EhCidadao())
        {
            if (user.IdCidadao() != id) throw new SemPermissaoException("Acesso negado aos dados de outro cidadão.");
            return cidadao;
        }
        if (user.EhProfissional())
        {
            var autorizado = await sessoes.ExisteAutorizadaDesdeAsync(id, DateTime.Now - JanelaConsentimento);
            if (!autorizado)
                throw new SemPermissaoException("Acesso restrito: é necessário acolhimento autorizado pelo cidadão nos últimos 10 minutos.");
            return cidadao;
        }
        throw new SemPermissaoException("Perfil sem permissão para este recurso.");
    }

    private static AlertaResumo ToResumo(Alerta a) =>
        new(a.IdAlerta, a.TipoAlerta, a.Severidade, a.Mensagem, a.StatusEnvio, a.CriadoEm);
}

// ============================================================
// Documentos da carteira (E8, E9): só metadados e hash (LGPD, SDD 8.6)
// ============================================================
public interface IDocumentoService
{
    Task<DocumentoCriadoResposta> CriarAsync(DocumentoRequest req, ClaimsPrincipal user);
    Task ExcluirAsync(long idDocumento, ClaimsPrincipal user);
}

public class DocumentoService(IDocumentoRepository documentos) : IDocumentoService
{
    private static readonly HashSet<string> TiposValidos =
        ["RG", "CPF", "CERTIDAO_NASCIMENTO", "CARTAO_VACINA", "RECEITA_MEDICA", "LAUDO"];

    public async Task<DocumentoCriadoResposta> CriarAsync(DocumentoRequest req, ClaimsPrincipal user)
    {
        var idCidadao = DonoCidadao(user);
        if (!TiposValidos.Contains(req.TipoDocumento))
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["tipoDocumento"] = "tipo de documento inválido" });
        if (req.TipoDocumento == "RECEITA_MEDICA" && req.DataValidade is null)
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["dataValidade"] = "obrigatória para RECEITA_MEDICA" });

        var doc = new CarteiraMetadados
        {
            IdCidadao = idCidadao,
            TipoDocumento = req.TipoDocumento,
            NomeArquivo = req.NomeArquivo,
            HashIntegridade = req.HashIntegridade.ToLowerInvariant(),
            DataValidade = req.DataValidade,
            DataCadastro = DateTime.Now
        };
        await documentos.AdicionarAsync(doc);
        await documentos.SalvarAsync();
        return new DocumentoCriadoResposta(doc.IdDocumento, "Documento registrado na carteira.");
    }

    public async Task ExcluirAsync(long idDocumento, ClaimsPrincipal user)
    {
        var idCidadao = DonoCidadao(user);
        var doc = await documentos.PorIdAsync(idDocumento)
            ?? throw new NaoEncontradoException("Documento não encontrado.");
        if (doc.IdCidadao != idCidadao)
            throw new SemPermissaoException("Documento pertence a outro cidadão.");
        await documentos.RemoverAsync(doc);
        await documentos.SalvarAsync();
    }

    private static long DonoCidadao(ClaimsPrincipal user) =>
        user.EhCidadao() && user.IdCidadao() is { } id
            ? id
            : throw new SemPermissaoException("Apenas o próprio cidadão gerencia a carteira.");
}

// ============================================================
// Unidades (E13)
// ============================================================
public interface IUnidadeService
{
    Task<PaginaResposta<UnidadeResumo>> ListarAsync(int page, int size);
}

public class UnidadeService(IUnidadeRepository unidades) : IUnidadeService
{
    public async Task<PaginaResposta<UnidadeResumo>> ListarAsync(int page, int size)
    {
        var (itens, total) = await unidades.ListarAtivasAsync(page, size);
        return new PaginaResposta<UnidadeResumo>(
            itens.Select(u => new UnidadeResumo(u.IdUnidade, u.CodigoCnes, u.NomeEstabelecimento,
                u.TipoUnidade, u.Bairro, u.Latitude, u.Longitude)).ToList(),
            page, size, total);
    }
}

// ============================================================
// Auditoria (E14)
// ============================================================
public interface IAuditoriaService
{
    Task<PaginaResposta<AuditoriaItem>> ListarAsync(string? status, DateOnly? de, DateOnly? ate, int page, int size);
}

public class AuditoriaService(ISessaoRepository sessoes, CartaoSus.Api.Data.CartaoSusDbContext db) : IAuditoriaService
{
    public async Task<PaginaResposta<AuditoriaItem>> ListarAsync(string? status, DateOnly? de, DateOnly? ate, int page, int size)
    {
        var (itens, total) = await sessoes.AuditoriaAsync(status,
            de?.ToDateTime(TimeOnly.MinValue), ate?.AddDays(1).ToDateTime(TimeOnly.MinValue), page, size);

        var cidadaoIds = itens.Select(s => s.IdCidadao).Distinct().ToList();
        var unidadeIds = itens.Select(s => s.IdUnidade).Distinct().ToList();
        var cidadaos = db.Cidadaos.Where(c => cidadaoIds.Contains(c.IdCidadao))
            .Select(c => new { c.IdCidadao, c.NomeCompleto, Login = c.Usuario!.Login }).ToList();
        var nomes = db.Unidades.Where(u => unidadeIds.Contains(u.IdUnidade))
            .ToDictionary(u => u.IdUnidade, u => u.NomeEstabelecimento);

        var lista = itens.Select(s =>
        {
            var c = cidadaos.FirstOrDefault(x => x.IdCidadao == s.IdCidadao);
            return new AuditoriaItem(s.IdSessao, c?.NomeCompleto ?? "", c?.Login,
                nomes.GetValueOrDefault(s.IdUnidade, ""), s.TokenUtilizado,
                s.StatusAutenticacao, s.Motivo, s.DataHoraAcesso);
        }).ToList();
        return new PaginaResposta<AuditoriaItem>(lista, page, size, total);
    }
}
