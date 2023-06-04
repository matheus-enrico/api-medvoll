package med.voll.api.domain.consulta;

import med.voll.api.domain.ValidacaoException;
import med.voll.api.domain.consulta.validacoes.ValidadorAgendamentoDeConsultas;
import med.voll.api.domain.medico.Medico;
import med.voll.api.domain.paciente.Paciente;
import med.voll.api.domain.repository.MedicoRepository;
import med.voll.api.domain.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendaDeConsultas {
    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private List<ValidadorAgendamentoDeConsultas> validadores;

    public DadosDetalhamentoConsulta agendar(DadosAgendamentoConsulta dados) {

        if (!pacienteRepository.existsById(dados.idPaciente())){
            throw new ValidacaoException("O id do paciente não existe!");
        }
        if (dados.idMedico() != null && !medicoRepository.existsById(dados.idMedico())){
            throw new ValidacaoException("O id do medico não existe!");
        }

        validadores.forEach(implementacao -> implementacao.validar(dados));

        Paciente paciente = pacienteRepository.getReferenceById(dados.idPaciente());
        Medico medico = escolherMedico(dados);
        if (medico == null){
            throw new ValidacaoException("Não existe médico disponivel nessa data!");
        }
        Consulta consulta = new Consulta(null, medico, paciente, dados.data(), null);
        consultaRepository.save(consulta);

        return new DadosDetalhamentoConsulta(consulta);
    }

    private Medico escolherMedico(DadosAgendamentoConsulta dados) {
        if (dados.idMedico() != null) {
            return medicoRepository.getReferenceById(dados.idMedico());
        }
        if (dados.especialidade() == null) {
            throw new ValidacaoException("Especialidade é obrigatoria quando o medico não for escolhido!");
        }

        return  medicoRepository.escolherMedicoAleatorioLivre(dados.especialidade(), dados.data());
    }


    public void excluirConsulta(DadosCancelamentoConsulta dados) {
        if (!consultaRepository.existsById(dados.idConsulta())){
            throw new ValidacaoException("Id da consulta informado não existe!");
        }

        Consulta consulta = consultaRepository.getReferenceById(dados.idConsulta());

        LocalDateTime dataConsulta = consulta.getData();
        LocalDateTime agora = LocalDateTime.now();
        long diferencaEmMinutos = Duration.between(agora, dataConsulta).toHours();

        if (diferencaEmMinutos < 24) {
            throw new ValidacaoException("Uma consulta somente poderá ser cancelada com antecedência mínima de 24 horas!");
        }
        consulta.cancelar(dados.motivo());
        consultaRepository.delete(consulta);
    }

}
