package br.edu.utfpr.pb.pw26s.server.service;

import br.edu.utfpr.pb.pw26s.server.model.Product;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface ProductService extends CrudService<Product, Long> {

    Product save(Product entity, MultipartFile file);

    void downloadFile(Long id, HttpServletResponse response);
}
