package com.mateo_baccillere.orders;

import com.mateo_baccillere.orders.client.ProductClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {"product-service.base-url=http://localhost:8083" })
class OrdersApplicationTests {


	@MockitoBean
	private ProductClient productClient;

	@Test
	void contextLoads() {
	}

}
