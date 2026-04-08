package service;

import com.mateo_baccillere.products.client.UserClient;
import com.mateo_baccillere.products.dto.CreateProductRequest;
import com.mateo_baccillere.products.dto.ProductResponse;
import com.mateo_baccillere.products.dto.UserResponse;
import com.mateo_baccillere.products.entity.Product;
import com.mateo_baccillere.products.exception.BusinessException;
import com.mateo_baccillere.products.exception.ProductNotFoundException;
import com.mateo_baccillere.products.repository.ProductRepository;
import com.mateo_baccillere.products.service.ProductService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ProductServiceTest {
    @Test
    void shouldCreateProductSuccessfully() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository, userClient);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setRole("SELLER");
        userResponse.setActive(true);

        CreateProductRequest request = buildValidRequest();

        when(userClient.getUserById(anyLong())).thenReturn(userResponse);
        when(repository.existsByNameIgnoreCase("Keyboard")).thenReturn(false);
        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.create(request);

        assertThat(response.getName()).isEqualTo("Keyboard");
        assertThat(response.getPrice()).isEqualByComparingTo("99.99");
        assertThat(response.getStock()).isEqualTo(10);
        assertThat(response.getActive()).isTrue();

        verify(userClient).getUserById(request.getSellerId());
        verify(repository).save(any(Product.class));
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        CreateProductRequest request = buildValidRequest();
        request.setName("   ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product name cannot be blank");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenPriceIsZero() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        CreateProductRequest request = buildValidRequest();
        request.setPrice(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product price must be greater than zero");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenStockIsNegative() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        CreateProductRequest request = buildValidRequest();
        request.setStock(-1);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product stock cannot be negative");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameAlreadyExists() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        CreateProductRequest request = buildValidRequest();
        when(repository.existsByNameIgnoreCase("Keyboard")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product name already exists");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateStockSuccessfully() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        Product product = new Product(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                1L,
                10,
                true
        );

        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.updateStock(1L, 5);

        assertThat(response.getStock()).isEqualTo(5);
    }

    @Test
    void shouldFailWhenUpdatingNegativeStock() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        assertThatThrownBy(() -> service.updateStock(1L, -5))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product stock cannot be negative");

        verify(repository, never()).findById(anyLong());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductNotFound() {
        ProductRepository repository = mock(ProductRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductService service = new ProductService(repository,userClient);

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found with id: 999");
    }

    private CreateProductRequest buildValidRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Keyboard");
        request.setDescription("Mechanical keyboard");
        request.setPrice(new BigDecimal("99.99"));
        request.setStock(10);
        request.setActive(true);
        request.setSellerId(1L);
        return request;
    }
}
