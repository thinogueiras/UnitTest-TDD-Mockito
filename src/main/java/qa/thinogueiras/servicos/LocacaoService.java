package qa.thinogueiras.servicos;

import static qa.thinogueiras.utils.DataUtils.adicionarDias;
import static qa.thinogueiras.utils.DataUtils.obterDataComDiferencaDias;
import static qa.thinogueiras.utils.DataUtils.verificarDiaSemana;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import qa.thinogueiras.dao.LocacaoDAO;
import qa.thinogueiras.entidades.Filme;
import qa.thinogueiras.entidades.Locacao;
import qa.thinogueiras.entidades.Usuario;
import qa.thinogueiras.exceptions.LocadoraException;

public class LocacaoService {
	
	private LocacaoDAO dao;
	private SPCService spcService;
	private EmailService emailService;
	
	public Locacao alugarFilme(Usuario usuario, List<Filme> filmes) throws LocadoraException {		
		if(usuario == null) {
			throw new LocadoraException("Usuário vazio");
		}		
		
		if(filmes == null || filmes.isEmpty()) {
			throw new LocadoraException("Sem filmes");
		}
		
		for(Filme filme: filmes) {
			if(filme.getEstoque() == 0) {
				throw new LocadoraException("Estoque vazio");
			}			
		}
		
		for(Filme filme: filmes) {
			if(filme.getPrecoLocacao() == 0) {
				throw new LocadoraException("Filme sem preço");
			}			
		}
		
		boolean negativado;
		
		try {
			negativado = spcService.getStatusSPC(usuario);				
		} catch (Exception e) {
			throw new LocadoraException("Erro na consulta ao SPC");			
		}
		
		if(negativado) {
			throw new LocadoraException("Usuário negativado");
		}
		
		Locacao locacao = new Locacao();
		locacao.setFilmes(filmes);
		locacao.setUsuario(usuario);
		locacao.setDataLocacao(new Date());
		Double valorTotal = 0d;
		
		for(int i = 0; i < filmes.size(); i++) {
			Filme filme = filmes.get(i);
			Double valorFilme = filme.getPrecoLocacao();
			
			switch (i) {
				case 2: valorFilme *= 0.75; break;
				case 3: valorFilme *= 0.50; break;
				case 4: valorFilme *= 0.25; break;
				case 5: valorFilme *= 0.00; break;			
			}
			
			valorTotal += valorFilme;			
		}
		
		locacao.setValor(valorTotal);
		
		Date dataEntrega = new Date();
		dataEntrega = adicionarDias(dataEntrega, 1);
		
		if(verificarDiaSemana(dataEntrega, Calendar.SUNDAY)) {
			dataEntrega = adicionarDias(dataEntrega, 1);
		}
		
		locacao.setDataRetorno(dataEntrega);		
		dao.salvar(locacao);
		
		return locacao;		
	}
	
	public void notificarAtrasosNaDevolucao() {
		List<Locacao> locacoes = dao.obterLocacoesPendentes();
		for(Locacao locacao: locacoes) {
			if(locacao.getDataRetorno().before(new Date())) {
				emailService.notificarAtrasoLocacao(locacao.getUsuario());				
			}
		}
	}
	
	public void prorrogarLocacao(Locacao locacao, int dias) {
		Locacao novaLocacao = new Locacao();
		novaLocacao.setUsuario(locacao.getUsuario());
		novaLocacao.setFilmes(locacao.getFilmes());
		novaLocacao.setDataLocacao(new Date());
		novaLocacao.setDataRetorno(obterDataComDiferencaDias(dias));
		novaLocacao.setValor(locacao.getValor() * dias);
		dao.salvar(novaLocacao);
	}
}