package dev.eduardo.artemedica.farmacia.model;

import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Empleado medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstatusSolicitud estatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmaceutico_id")
    private Empleado farmaceutico;

    @Column
    private LocalDateTime fechaAprobacion;

    @Column
    private LocalDateTime fechaEntrega;

    @Column
    private String motivoRechazo;

    @Column
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL)
    private List<SolicitudDetalle> detalles;

    @Version
    private Long version;
}
