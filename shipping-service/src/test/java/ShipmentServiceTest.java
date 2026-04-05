

import com.mateo_baccillere.shipping.client.NotificationClient;
import com.mateo_baccillere.shipping.client.OrderClient;
import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.domain.Shipment;
import com.mateo_baccillere.shipping.domain.ShipmentStatus;
import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.exception.DuplicateShipmentException;
import com.mateo_baccillere.shipping.exception.InvalidOrderStateException;
import com.mateo_baccillere.shipping.exception.InvalidShipmentStateTransitionException;
import com.mateo_baccillere.shipping.exception.ShipmentNotFoundException;
import com.mateo_baccillere.shipping.repository.ShipmentRepository;
import com.mateo_baccillere.shipping.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private ShipmentService shipmentService;

    private CreateShipmentRequest createShipmentRequest;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        createShipmentRequest = new CreateShipmentRequest(
                1L,
                "Mateo Baccillere",
                "Cordoba, Argentina"
        );

        shipment = Shipment.builder()
                .id(10L)
                .orderId(1L)
                .customerName("Mateo Baccillere")
                .shippingAddress("Cordoba, Argentina")
                .status(ShipmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateShipmentWhenOrderIsConfirmed() {
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);
        when(orderClient.getOrderById(1L)).thenReturn(new OrderResponse(1L, "Mateo Baccillere", "CONFIRMED"));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        ShipmentResponse response = shipmentService.createShipment(createShipmentRequest);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.orderId());
        assertEquals(ShipmentStatus.PENDING, response.status());

        verify(shipmentRepository).existsByOrderId(1L);
        verify(orderClient).getOrderById(1L);
        verify(shipmentRepository).save(any(Shipment.class));
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void shouldThrowDuplicateShipmentExceptionWhenShipmentAlreadyExists() {
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThrows(
                DuplicateShipmentException.class,
                () -> shipmentService.createShipment(createShipmentRequest)
        );

        verify(shipmentRepository).existsByOrderId(1L);
        verify(orderClient, never()).getOrderById(anyLong());
        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void shouldThrowInvalidOrderStateExceptionWhenOrderIsNotConfirmed() {
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);
        when(orderClient.getOrderById(1L)).thenReturn(new OrderResponse(1L, "Mateo Baccillere", "CREATED"));

        assertThrows(
                InvalidOrderStateException.class,
                () -> shipmentService.createShipment(createShipmentRequest)
        );

        verify(shipmentRepository).existsByOrderId(1L);
        verify(orderClient).getOrderById(1L);
        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void shouldReturnShipmentById() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        ShipmentResponse response = shipmentService.getShipmentById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.orderId());
        assertEquals(ShipmentStatus.PENDING, response.status());
    }

    @Test
    void shouldThrowShipmentNotFoundExceptionWhenGettingById() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentById(10L)
        );
    }

    @Test
    void shouldReturnShipmentByOrderId() {
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.of(shipment));

        ShipmentResponse response = shipmentService.getShipmentByOrderId(1L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.orderId());
    }

    @Test
    void shouldMarkShipmentReadyForDelivery() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.markReadyForDelivery(10L);

        assertEquals(ShipmentStatus.READY_FOR_DELIVERY, response.status());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void shouldThrowInvalidTransitionWhenMarkingReadyFromInvalidStatus() {
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        assertThrows(
                InvalidShipmentStateTransitionException.class,
                () -> shipmentService.markReadyForDelivery(10L)
        );

        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void shouldMarkShipmentInTransit() {
        shipment.setStatus(ShipmentStatus.READY_FOR_DELIVERY);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.markInTransit(10L);

        assertEquals(ShipmentStatus.IN_TRANSIT, response.status());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void shouldMarkShipmentDeliveredAndShipOrder() {
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.markDelivered(10L);

        assertEquals(ShipmentStatus.DELIVERED, response.status());
        verify(notificationClient).sendNotification(any());
        verify(orderClient).markOrderAsShipped(1L);
    }

    @Test
    void shouldMarkShipmentFailed() {
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.markFailed(10L);

        assertEquals(ShipmentStatus.FAILED, response.status());
        verify(notificationClient).sendNotification(any());
        verify(orderClient, never()).markOrderAsShipped(anyLong());
    }

    @Test
    void shouldCancelShipmentFromPending() {
        shipment.setStatus(ShipmentStatus.PENDING);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.cancelShipment(10L);

        assertEquals(ShipmentStatus.CANCELLED, response.status());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void shouldCancelShipmentFromReadyForDelivery() {
        shipment.setStatus(ShipmentStatus.READY_FOR_DELIVERY);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.cancelShipment(10L);

        assertEquals(ShipmentStatus.CANCELLED, response.status());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void shouldThrowInvalidTransitionWhenCancellingFromDelivered() {
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        assertThrows(
                InvalidShipmentStateTransitionException.class,
                () -> shipmentService.cancelShipment(10L)
        );

        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void shouldSendShipmentCreatedNotificationWithCorrectPayload() {
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);
        when(orderClient.getOrderById(1L)).thenReturn(new OrderResponse(1L, "Mateo Baccillere", "CONFIRMED"));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        shipmentService.createShipment(createShipmentRequest);

        ArgumentCaptor<com.mateo_baccillere.shipping.dto.NotificationRequest> captor =
                ArgumentCaptor.forClass(com.mateo_baccillere.shipping.dto.NotificationRequest.class);

        verify(notificationClient).sendNotification(captor.capture());

        assertEquals(10L, captor.getValue().referenceId());
        assertEquals("SHIPMENT_CREATED", captor.getValue().type());
    }
}
