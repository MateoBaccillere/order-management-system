package com.mateo_baccillere.orders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"product-service.base-url=http://localhost:8083" })
class OrdersApplicationTests {

	@Test
	void contextLoads() {
	}

}
