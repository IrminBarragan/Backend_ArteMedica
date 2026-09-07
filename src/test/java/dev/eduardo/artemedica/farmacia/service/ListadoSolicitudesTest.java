package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudResponseDTO;
import dev.eduardo.artemedica.farmacia.model.Area;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Solicitud;
import dev.eduardo.artemedica.farmacia.model.SolicitudDetalle;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;
import dev.eduardo.artemedica.farmacia.repository.AreaRepository;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudDetalleRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El coste de listar solicitudes no debe crecer con el tamano de la pagina.
 *
 * Se cuentan las consultas que Hibernate emite de verdad, porque es lo unico que distingue
 * una consulta con carga anticipada de veinte consultas perezosas que devuelven lo mismo.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@DisplayName("Listado de solicitudes")
class ListadoSolicitudesTest {

    @Autowired private SolicitudService solicitudService;
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private SolicitudDetalleRepository solicitudDetalleRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaMedicamentoRepository categoriaRepository;
    @Autowired private EntityManager entityManager;

    private Long medicoId;

    @BeforeEach
    void crearSolicitudes() {
        Empleado medico = empleadoRepository.save(Empleado.builder()
                .nombres("Laura").apellidoPaterno("Mendoza").tipo(TipoEmpleado.MEDICO).activo(true).build());
        medicoId = medico.getId();
        Area area = areaRepository.save(Area.builder().nombre("Urgencias").activo(true).build());
        CategoriaMedicamento cat = categoriaRepository.save(
                CategoriaMedicamento.builder().nombre("Analgesicos").activo(true).build());
        Producto producto = productoRepository.save(Producto.builder()
                .nombre("Paracetamol").presentacion("Tabletas").categoria(cat)
                .precioVenta(BigDecimal.ONE).precioCompra(BigDecimal.ONE)
                .stockMinimo(0).stockActual(500).activo(true).build());

        for (int i = 0; i < 20; i++) {
            Solicitud s = solicitudRepository.save(Solicitud.builder()
                    .medico(medico).area(area).fechaSolicitud(LocalDateTime.now().minusMinutes(i))
                    .estatus(EstatusSolicitud.PENDIENTE).createdAt(LocalDateTime.now()).build());
            solicitudDetalleRepository.save(SolicitudDetalle.builder()
                    .solicitud(s).producto(producto).cantidadSolicitada(5).cantidadEntregada(0).build());
        }
        entityManager.flush();
    }

    @Test
    @DisplayName("una pagina de 20 solicitudes cuesta un numero fijo y pequeno de consultas")
    void listarNoDisparaConsultaPorSolicitud() {
        Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        entityManager.clear();
        stats.clear();

        PaginaDTO<SolicitudResponseDTO> pagina =
                solicitudService.listar(null, null, PageRequest.of(0, 20));

        long consultas = stats.getPrepareStatementCount();

        assertThat(pagina.contenido()).hasSize(20);
        assertThat(pagina.totalElementos()).isEqualTo(20);
        // Medido: 3 consultas. La de la pagina, la del conteo y la de todos los detalles.
        // Antes eran 1 de solicitudes, 20 de detalles y las perezosas de medico, area, lote y
        // producto de cada fila: mas de ochenta para el mismo resultado.
        assertThat(consultas)
                .as("consultas emitidas para armar la pagina")
                .isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("la pagina respeta el tamano pedido y reporta el total real")
    void paginaRespetaTamano() {
        PaginaDTO<SolicitudResponseDTO> primera =
                solicitudService.listar(null, null, PageRequest.of(0, 7));

        assertThat(primera.contenido()).hasSize(7);
        assertThat(primera.totalElementos()).isEqualTo(20);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(primera.primera()).isTrue();
        assertThat(primera.ultima()).isFalse();
    }

    @Test
    @DisplayName("la ultima pagina trae el resto y se marca como ultima")
    void ultimaPagina() {
        PaginaDTO<SolicitudResponseDTO> ultima =
                solicitudService.listar(null, null, PageRequest.of(2, 7));

        assertThat(ultima.contenido()).hasSize(6);
        assertThat(ultima.ultima()).isTrue();
    }

    @Test
    @DisplayName("el filtro por medico se aplica sobre la consulta paginada")
    void filtraPorMedico() {
        PaginaDTO<SolicitudResponseDTO> propias =
                solicitudService.listar(null, medicoId, PageRequest.of(0, 20));
        assertThat(propias.totalElementos()).isEqualTo(20);

        PaginaDTO<SolicitudResponseDTO> ajenas =
                solicitudService.listar(null, medicoId + 999, PageRequest.of(0, 20));
        assertThat(ajenas.totalElementos()).isZero();
    }

    @Test
    @DisplayName("los detalles llegan completos en cada solicitud de la pagina")
    void detallesCompletos() {
        PaginaDTO<SolicitudResponseDTO> pagina =
                solicitudService.listar(null, null, PageRequest.of(0, 20));

        assertThat(pagina.contenido()).allSatisfy(s -> {
            assertThat(s.detalles()).hasSize(1);
            assertThat(s.detalles().getFirst().productoNombre()).isEqualTo("Paracetamol");
            assertThat(s.medicoNombre()).isEqualTo("Laura Mendoza");
            assertThat(s.areaNombre()).isEqualTo("Urgencias");
        });
    }
}
