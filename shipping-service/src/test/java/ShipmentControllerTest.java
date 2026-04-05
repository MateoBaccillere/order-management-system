
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mateo_baccillere.shipping.controller.ShipmentController;
import com.mateo_baccillere.shipping.domain.ShipmentStatus;
import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.exception.GlobalExceptionHandler;
import com.mateo_baccillere.shipping.exception.ShipmentNotFoundException;
import com.mateo_baccillere.shipping.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class ShipmentControllerTest {
    private final ShipmentService shipmentService = mock(ShipmentService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ShipmentController(shipmentService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateShipment() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina"
        );

        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.createShipment(any(CreateShipmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/shipments/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetShipmentById() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.getShipmentById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/shipments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenShipmentNotFound() throws Exception {
        when(shipmentService.getShipmentById(10L))
                .thenThrow(new ShipmentNotFoundException(10L));

        mockMvc.perform(get("/api/shipments/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldMarkShipmentReadyForDelivery() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.READY_FOR_DELIVERY,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.markReadyForDelivery(10L)).thenReturn(response);

        mockMvc.perform(patch("/api/shipments/10/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_DELIVERY"));
    }

    @Test
    void shouldMarkShipmentInTransit() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.IN_TRANSIT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.markInTransit(10L)).thenReturn(response);

        mockMvc.perform(patch("/api/shipments/10/in-transit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void shouldMarkShipmentDelivered() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.DELIVERED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.markDelivered(10L)).thenReturn(response);

        mockMvc.perform(patch("/api/shipments/10/deliver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void shouldMarkShipmentFailed() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.FAILED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.markFailed(10L)).thenReturn(response);

        mockMvc.perform(patch("/api/shipments/10/fail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void shouldCancelShipment() throws Exception {
        ShipmentResponse response = new ShipmentResponse(
                10L,
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina",
                ShipmentStatus.CANCELLED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(shipmentService.cancelShipment(10L)).thenReturn(response);

        mockMvc.perform(patch("/api/shipments/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
