package com.mash.api.service;

import com.mash.api.entity.BidParameter;
import com.mash.api.entity.Product;
import com.mash.api.entity.ProductAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ProductService {

    /**
     *
     * @param product
     * @param bidParameter
     * @param productAttribute
     * @param cycles
     * @param cyclesPrice
     * @param img1Path
     * @param img2Path
     * @param vendorId
     * @param positionId
     * @return
     */
    Product save(Product product,
                 BidParameter bidParameter,
                 ProductAttribute productAttribute,
                 String cycles,
                 String cyclesPrice,
                 String img1Path,
                 String img2Path,
                 Integer vendorId,
                 Integer positionId);

    Product findById(Integer id);

    List<Product> findAll();

    List<Product> findByAccountId(Integer accountId);

    Page<Product> findByAccountId(Pageable pageable, Integer accountId);

    List<Product> findByProductType(String productType);

    List<Product> findByPriceType(Integer priceType);

    List<Product> findByAccountIdAndPriceType(Integer accountId, Integer priceType);

    Page<Product> findByAccountIdAndPriceType(Pageable pageable, Integer accountId, Integer priceType);

    /**
     * 条件查询
     * @param specification
     * @param pageable
     * @return
     */
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    /**
     * 前台地图搜索用
     * @param specification
     * @return
     */
    List<Product> findAll(Specification<Product> specification);

    List<Integer> findProductId();

    /**
     * 热门广告位
     * @param vendorId
     * @return
     */
    List<Product> hotProduct(Integer vendorId);

    /**
     * 删除广告位
     * @param productId
     */
    void delete(Integer productId);

    /**
     * 查询站点下所有广告位
     * @param positionId
     * @return
     */
    List<Product> findByPositionId(Integer positionId);

    List<Product> findByAccountIdAndNumber(Integer accountId, String number);

    Product save2(Product product);
}
