package com.wa.finance.web.mobile;

public class InvalidClientAddressException extends RuntimeException {

    public InvalidClientAddressException() {
        super("Endereço remoto fornecido pela infraestrutura é inválido");
    }
}
