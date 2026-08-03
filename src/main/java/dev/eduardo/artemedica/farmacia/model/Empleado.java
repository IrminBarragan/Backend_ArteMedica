package dev.eduardo.artemedica.farmacia.model;

import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidoPaterno;

    @Column
    private String apellidoMaterno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEmpleado tipo;

    @Column
    private String especialidad;

    @Column
    private String cedulaProfesional;

    @Column
    private String telefonoGuardia;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
