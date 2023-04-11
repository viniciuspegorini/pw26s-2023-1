package br.edu.utfpr.pb.pw26s.server.controller;

import br.edu.utfpr.pb.pw26s.server.dto.ProductDto;
import br.edu.utfpr.pb.pw26s.server.model.Product;
import br.edu.utfpr.pb.pw26s.server.service.CrudService;
import br.edu.utfpr.pb.pw26s.server.service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RestController
@RequestMapping("products")
public class ProductController extends CrudController<Product, ProductDto, Long> {

    private static final String FILE_PATH = File.separator + "uploads";
    private final ProductService productService;
    private final ModelMapper modelMapper;


    public ProductController(ProductService productService, ModelMapper modelMapper) {
        super(Product.class, ProductDto.class);
        this.productService = productService;
        this.modelMapper = modelMapper;
    }

    @Override
    protected CrudService<Product, Long> getService() {
        return this.productService;
    }

    @Override
    protected ModelMapper getModelMapper() {
        return this.modelMapper;
    }

    /* Upload de arquivo salvo no sistema de arquivos

    formData = { product:{}, image:"arquivo"}
     */
    @PostMapping("upload-a")
    public Product save(@RequestPart("product") @Valid Product product,
                        @RequestPart("image") MultipartFile file) {
        getService().save(product);
        productService.saveImage(file, product);
        return product;
    }

    // Upload de arquivo salvo no Banco de dados
    @PostMapping("upload-b")
    public Product saveImageFile(@RequestPart("product") @Valid Product product,
                                 @RequestPart("image") MultipartFile file) {
        getService().save(product);
        productService.saveImageFile(file, product);
        return product;
    }

}
