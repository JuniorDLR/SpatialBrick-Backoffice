package ni.spatialBrick.SpatialBrick.modelo.enumeraciones;

import lombok.Getter;

@Getter
public enum DuracionMinutos {
    _5_MINUTOS(5),
    _10_MINUTOS(10),
    _15_MINUTOS(15),
    _20_MINUTOS(20),
    _25_MINUTOS(25),
    _30_MINUTOS(30),
    _35_MINUTOS(35),
    _40_MINUTOS(40),
    _45_MINUTOS(45),
    _50_MINUTOS(50),
    _55_MINUTOS(55),
    _60_MINUTOS(60);

    private final int valor;

    DuracionMinutos(int valor) {
        this.valor = valor;
    }
}
