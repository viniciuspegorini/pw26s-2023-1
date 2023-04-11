package br.edu.utfpr.pb.pw26s.server.service.impl;

import br.edu.utfpr.pb.pw26s.server.model.Product;
import br.edu.utfpr.pb.pw26s.server.repository.ProductRepository;
import br.edu.utfpr.pb.pw26s.server.service.ProductService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class ProductServiceImpl extends CrudServiceImpl<Product, Long>
    implements ProductService {

    private static final String FILE_PATH = File.separator + "uploads";

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    protected JpaRepository<Product, Long> getRepository() {
        return this.productRepository;
    }


    private String encodeFileToBase64(String filename) throws IOException {
        File file = new File(filename);
        FileInputStream stream = new FileInputStream(file);
        byte[] encoded = Base64.encodeBase64(IOUtils.toByteArray(stream));
        stream.close();
        return new String(encoded, StandardCharsets.US_ASCII);
    }

}
