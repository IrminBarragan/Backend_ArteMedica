package dev.eduardo.artemedica.farmacia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String presentacion;

    @Column(unique = true)
    private String codigoBarras;

    @Column(nullable = false)
    @Builder.Default
    private boolean esControlado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaMedicamento categoria;

    @Column
    private BigDecimal precioVenta;

    @Column
    private BigDecimal precioCompra;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
