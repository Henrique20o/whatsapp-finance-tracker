package com.whatsapp_service.service;

import com.whatsapp_service.dto.GastoPorCategoriaDTO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;

@Service
public class PieChartService {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;
    private static final Color[] COLORS = {
            new Color(52, 152, 219),
            new Color(46, 204, 113),
            new Color(241, 196, 15),
            new Color(231, 76, 60),
            new Color(155, 89, 182),
            new Color(26, 188, 156),
            new Color(230, 126, 34),
            new Color(127, 140, 141)
    };

    public String gerarGraficoBase64(List<GastoPorCategoriaDTO> gastos) {
        if (gastos == null || gastos.isEmpty()) {
            throw new IllegalArgumentException("Não há gastos para gerar o gráfico");
        }

        BigDecimal total = gastos.stream()
                .map(GastoPorCategoriaDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() <= 0) {
            throw new IllegalArgumentException("O total de gastos deve ser maior que zero");
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            prepararCanvas(graphics);
            desenharTitulo(graphics);
            desenharPizza(graphics, gastos, total);
            desenharLegenda(graphics, gastos, total);
        } finally {
            graphics.dispose();
        }

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(toPng(image));
    }

    private void prepararCanvas(Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void desenharTitulo(Graphics2D graphics) {
        graphics.setColor(new Color(33, 37, 41));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.drawString("Gastos por categoria — últimos 30 dias", 55, 55);
    }

    private void desenharPizza(Graphics2D graphics, List<GastoPorCategoriaDTO> gastos, BigDecimal total) {
        int inicio = 90;
        int acumulado = 0;

        for (int index = 0; index < gastos.size(); index++) {
            int arco = index == gastos.size() - 1
                    ? 360 - acumulado
                    : gastos.get(index).total()
                            .multiply(BigDecimal.valueOf(360))
                            .divide(total, 0, RoundingMode.HALF_UP)
                            .intValue();

            graphics.setColor(COLORS[index % COLORS.length]);
            graphics.fillArc(55, 105, 410, 410, inicio, -arco);
            inicio -= arco;
            acumulado += arco;
        }
    }

    private void desenharLegenda(Graphics2D graphics, List<GastoPorCategoriaDTO> gastos, BigDecimal total) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        int y = 130;

        for (int index = 0; index < gastos.size(); index++) {
            GastoPorCategoriaDTO gasto = gastos.get(index);
            BigDecimal percentual = gasto.total()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP);

            graphics.setColor(COLORS[index % COLORS.length]);
            graphics.fillRoundRect(520, y - 17, 24, 24, 5, 5);
            graphics.setColor(new Color(33, 37, 41));
            graphics.drawString(
                    limitar(gasto.categoria(), 22) + " — " + percentual.toPlainString() + "%",
                    558,
                    y + 2
            );
            y += 45;
        }
    }

    private String limitar(String texto, int tamanho) {
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho - 1) + "…";
    }

    private byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gerar o gráfico", e);
        }
    }
}
