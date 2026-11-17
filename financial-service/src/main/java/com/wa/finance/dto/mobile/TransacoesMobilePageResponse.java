package com.wa.finance.dto.mobile;

import java.util.List;

public record TransacoesMobilePageResponse(
        List<TransacaoMobileResponse> itens,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) { }
