package com.example.bezma.service;

import com.example.bezma.common.res.PageResponse;
import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import com.example.bezma.entity.inventory.Product;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.repository.ProductRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.service.CloudinaryService;
import com.example.bezma.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Tenant testTenant;
    private Product product1;
    private Product product2;
    private Product productOutOfStock;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .name("Test Company")
                .tenantCode("TC001")
                .slug("test-company")
                .phone("0123456789")
                .email("company@test.com")
                .build();

        product1 = Product.builder()
                .id(1L)
                .name("Laptop Dell XPS 15")
                .sku("DELL-XPS-15")
                .category("Laptop")
                .price(35000000.0)
                .stock(50)
                .imageUrl("https://cdn.example.com/dell-xps.jpg")
                .tenant(testTenant)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        product2 = Product.builder()
                .id(2L)
                .name("iPhone 15 Pro Max")
                .sku("IP-15PM")
                .category("Điện thoại")
                .price(30000000.0)
                .stock(3)
                .imageUrl("https://cdn.example.com/iphone.jpg")
                .tenant(testTenant)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productOutOfStock = Product.builder()
                .id(3L)
                .name("MacBook Pro M3")
                .sku("MB-M3")
                .category("Laptop")
                .price(45000000.0)
                .stock(0)
                .tenant(testTenant)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET PRODUCTS (PAGINATED) ====================

    @Nested
    @DisplayName("Get Products (Paginated) Tests")
    class GetProductsPagedTests {

        @Test
        @DisplayName("Lấy danh sách sản phẩm phân trang → trả về PageResponse đầy đủ")
        void getProducts_NoFilter_ReturnsPagedProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    Arrays.asList(product1, product2, productOutOfStock),
                    pageable, 3);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, null, null, null, 0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Lọc theo danh mục → chỉ trả về sản phẩm đúng category")
        void getProducts_FilterByCategory_ReturnsFilteredProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    Arrays.asList(product1, productOutOfStock),
                    pageable, 2);

            when(productRepository.findByTenantIdAndCategory(1L, "Laptop", pageable))
                    .thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, "Laptop", null, null, 0, 10);

            // Assert
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allSatisfy(p ->
                    assertThat(p.getCategory()).isEqualTo("Laptop"));
        }

        @Test
        @DisplayName("Tìm kiếm theo tên → lọc đúng sản phẩm")
        void getProducts_SearchByName_FiltersCorrectly() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    Arrays.asList(product1, product2, productOutOfStock),
                    pageable, 3);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, null, "Dell", null, 0, 10);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).contains("Dell");
        }

        @Test
        @DisplayName("Category = 'Tất cả' → không lọc theo category")
        void getProducts_CategoryAll_ReturnsAll() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    Arrays.asList(product1, product2), pageable, 2);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, "Tất cả", null, null, 0, 10);

            // Assert
            assertThat(result.getContent()).hasSize(2);
            verify(productRepository).findByTenantId(1L, pageable);
            verify(productRepository, never()).findByTenantIdAndCategory(anyLong(), anyString(), any());
        }
    }

    // ==================== STATUS MAPPING TESTS ====================

    @Nested
    @DisplayName("Product Status Mapping Tests")
    class StatusMappingTests {

        @Test
        @DisplayName("Stock = 0 → status = 'HẾT HÀNG'")
        void productStatus_ZeroStock_IsOutOfStock() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    List.of(productOutOfStock), pageable, 1);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, null, null, null, 0, 10);

            // Assert
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("HẾT HÀNG");
        }

        @Test
        @DisplayName("Stock <= 5 → status = 'SẮP HẾT'")
        void productStatus_LowStock_IsLowStock() {
            // Arrange (product2 has stock = 3)
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    List.of(product2), pageable, 1);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, null, null, null, 0, 10);

            // Assert
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("SẮP HẾT");
        }

        @Test
        @DisplayName("Stock > 5 → status = 'CÒN HÀNG'")
        void productStatus_NormalStock_IsInStock() {
            // Arrange (product1 has stock = 50)
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(
                    List.of(product1), pageable, 1);

            when(productRepository.findByTenantId(1L, pageable)).thenReturn(productPage);

            // Act
            PageResponse<ProductResponse> result = productService.getProductsByTenant(
                    1L, null, null, null, 0, 10);

            // Assert
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("CÒN HÀNG");
        }
    }

    // ==================== CREATE PRODUCT TESTS ====================

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Tạo sản phẩm không có ảnh → sử dụng imageUrl từ request")
        void createProduct_NoImage_UsesRequestImageUrl() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest();
            request.setName("Keyboard Mechanical");
            request.setSku("KB-MECH-01");
            request.setCategory("Phụ kiện");
            request.setPrice(1500000.0);
            request.setStock(100);
            request.setImageUrl("https://cdn.example.com/keyboard.jpg");
            request.setTenantId(1L);

            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));

            Product savedProduct = Product.builder()
                    .id(4L)
                    .name("Keyboard Mechanical")
                    .sku("KB-MECH-01")
                    .category("Phụ kiện")
                    .price(1500000.0)
                    .stock(100)
                    .imageUrl("https://cdn.example.com/keyboard.jpg")
                    .tenant(testTenant)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // Act
            ProductResponse response = productService.createProduct(request, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Keyboard Mechanical");
            assertThat(response.getSku()).isEqualTo("KB-MECH-01");
            assertThat(response.getPrice()).isEqualTo(1500000.0);
            assertThat(response.getStock()).isEqualTo(100);
            assertThat(response.getStatus()).isEqualTo("CÒN HÀNG");
            assertThat(response.getImageUrl()).isEqualTo("https://cdn.example.com/keyboard.jpg");

            verify(productRepository).save(any(Product.class));
            verify(cloudinaryService, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("Tạo sản phẩm có upload ảnh → upload lên Cloudinary")
        void createProduct_WithImage_UploadsToCloudinary() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest();
            request.setName("Mouse Gaming");
            request.setSku("MOUSE-G01");
            request.setCategory("Phụ kiện");
            request.setPrice(500000.0);
            request.setStock(200);
            request.setTenantId(1L);

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);

            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(cloudinaryService.uploadFile(mockFile, "products"))
                    .thenReturn("https://cloudinary.com/uploaded-mouse.jpg");

            Product savedProduct = Product.builder()
                    .id(5L)
                    .name("Mouse Gaming")
                    .sku("MOUSE-G01")
                    .category("Phụ kiện")
                    .price(500000.0)
                    .stock(200)
                    .imageUrl("https://cloudinary.com/uploaded-mouse.jpg")
                    .tenant(testTenant)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // Act
            ProductResponse response = productService.createProduct(request, mockFile);

            // Assert
            assertThat(response.getImageUrl()).isEqualTo("https://cloudinary.com/uploaded-mouse.jpg");
            verify(cloudinaryService).uploadFile(mockFile, "products");
        }

        @Test
        @DisplayName("Tạo sản phẩm tenant không tồn tại → throw RuntimeException")
        void createProduct_TenantNotFound_ThrowsException() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest();
            request.setTenantId(999L);

            when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(request, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tenant not found");
        }
    }

    // ==================== UPDATE PRODUCT TESTS ====================

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Cập nhật sản phẩm thành công → trả về ProductResponse đã cập nhật")
        void updateProduct_Success_ReturnsUpdatedProduct() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest();
            request.setName("Updated Laptop");
            request.setSku("UPD-01");
            request.setCategory("Laptop");
            request.setPrice(40000000.0);
            request.setStock(30);
            request.setImageUrl("https://cdn.example.com/updated.jpg");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Updated Laptop")
                    .sku("UPD-01")
                    .category("Laptop")
                    .price(40000000.0)
                    .stock(30)
                    .imageUrl("https://cdn.example.com/updated.jpg")
                    .tenant(testTenant)
                    .createdAt(product1.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            // Act
            ProductResponse response = productService.updateProduct(1L, 1L, request, null);

            // Assert
            assertThat(response.getName()).isEqualTo("Updated Laptop");
            assertThat(response.getPrice()).isEqualTo(40000000.0);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Cập nhật sản phẩm khác tenant → throw Unauthorized")
        void updateProduct_WrongTenant_ThrowsUnauthorized() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest();
            request.setName("Hack Product");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

            // Act & Assert - Tenant ID 999 khác với product1's tenant ID (1)
            assertThatThrownBy(() -> productService.updateProduct(1L, 999L, request, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }

        @Test
        @DisplayName("Cập nhật sản phẩm không tồn tại → throw RuntimeException")
        void updateProduct_NotFound_ThrowsException() {
            // Arrange
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(999L, 1L, new CreateProductRequest(), null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    // ==================== DELETE PRODUCT TESTS ====================

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Xóa sản phẩm thành công")
        void deleteProduct_Success_DeletesProduct() {
            // Arrange
            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

            // Act
            productService.deleteProduct(1L, 1L);

            // Assert
            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Xóa sản phẩm khác tenant → throw Unauthorized")
        void deleteProduct_WrongTenant_ThrowsUnauthorized() {
            // Arrange
            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(1L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }

        @Test
        @DisplayName("Xóa sản phẩm không tồn tại → throw RuntimeException")
        void deleteProduct_NotFound_ThrowsException() {
            // Arrange
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(999L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }
    }
}
