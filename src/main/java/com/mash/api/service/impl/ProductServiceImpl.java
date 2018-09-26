package com.mash.api.service.impl;

import com.mash.api.entity.*;
import com.mash.api.exception.MyException;
import com.mash.api.repository.*;
import com.mash.api.service.PositionService;
import com.mash.api.service.ProductService;
import com.mash.api.util.OssUtil;
import com.mash.api.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    EntityManager em;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductPriceRepository productPriceRepository;

    @Autowired
    private BidParameterRepository bidParameterRepository;

    @Autowired
    private ProductAttributeRepository productAttributeRepository;

    @Autowired
    private PositionService positionService;

    @Transactional
    @Override
    public Product save(Product product,
                        BidParameter bidParameter,
                        ProductAttribute productAttribute,
                        String cycles,
                        String cyclesPrice,
                        String img1Path,
                        String img2Path,
                        Integer vendorId,
                        Integer positionId) {

        // 保存广告位基本信息
        product.setAccountId(vendorId);
        product.setCreatedTime(new Date());
        product.setUpdateTime(new Date());
//        String[] locationArray = location.split(",");
//        product.setProvince(locationArray[0]);
//        product.setCity(locationArray[1]);
//        product.setArea(locationArray[2]);
//        product.setAddress(locationArray[3]+locationArray[4]);
        Position position = positionService.findById(positionId);
        // product.setName(position.getRoad() + "-" + position.getStation() + "-" + product.getNumber());
        product.setPosition(position);
        product = productRepository.save(product);
        log.info("保存product基本信息ok");

        // 保存图片信息
        if (!Tools.isEmpty(img1Path))
        {
            ProductImage productImage = new ProductImage();
            productImage.setPath(img1Path);
            productImage.setProduct(product);
            productImageRepository.save(productImage);
        }

        if (!Tools.isEmpty(img2Path))
        {
            ProductImage productImage = new ProductImage();
            productImage.setPath(img2Path);
            productImage.setProduct(product);
            productImageRepository.save(productImage);
        }
        log.info("保存image基本信息ok");

        // 保存属性
        productAttribute.setProduct(product);
        productAttributeRepository.save(productAttribute);

        // 保存价格
        if (product.getPriceType() == 0)
        {
            log.info("保存周期价格start");
            // 保存周期价格
            String[] cycleArray = cycles.split(",");
            String[] cyclePriceArray = cyclesPrice.split(",");
            log.info("cycleArray = {}", cycleArray);
            log.info("cyclePriceArray = {}", cyclePriceArray);
            for (int i = 0; i < cycleArray.length; i ++)
            {
                ProductPrice productPrice = new ProductPrice();
                productPrice.setProduct(product);
                productPrice.setCycle(cycleArray[i]);
                productPrice.setPrice(Float.valueOf(cyclePriceArray[i]));

                productPriceRepository.save(productPrice);
            }
            log.info("保存周期价格end");
        }
        else
        {
            // 保存竞价规则
            bidParameter.setProduct(product);
            bidParameterRepository.save(bidParameter);
            log.info("保存竞价规则end");
        }
        return product;
    }

    @Override
    public Product findById(Integer id) {
        return productRepository.findOne(id);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findByProductType(String productType) {
        return productRepository.findByProductTypeAndPriceType(productType,0);
    }

    @Override
    public List<Product> findByAccountId(Integer accountId) {
        return productRepository.findByAccountId(accountId);
    }

    @Override
    public List<Product> findByPriceType(Integer priceType) {
        return productRepository.findByPriceType(priceType);
    }

    @Override
    public List<Product> findByAccountIdAndPriceType(Integer accountId, Integer priceType) {
        return productRepository.findByAccountIdAndPriceType(accountId, priceType);
    }

    @Override
    public Page<Product> findByAccountId(Pageable pageable, Integer accountId) {
        return productRepository.findByAccountId(pageable, accountId);
    }

    @Override
    public Page<Product> findByAccountIdAndPriceType(Pageable pageable, Integer accountId, Integer priceType) {
        return productRepository.findByAccountIdAndPriceType(pageable, accountId, priceType);
    }

    @Override
    public Page<Product> findAll(Specification<Product> specification, Pageable pageable) {

        return productRepository.findAll(specification, pageable);
    }

    @Override
    public List<Product> findAll(Specification<Product> specification) {
        return productRepository.findAll(specification);
    }

    @Override
    public List<Integer> findProductId() {
        return productRepository.findProductId();
    }

    @Override
    public List<Product> hotProduct(Integer vendorId) {
        return productRepository.hotProduct(vendorId);
    }

    @Transactional
    @Override
    public void delete(Integer productId){

        Product product = productRepository.findOne(productId);
        Set<ProductImage> productImages = product.getImages();

        // step1 删除产品图片 product_image
        productImageRepository.deleteByProductId(productId);
        Iterator<ProductImage> productImageIterator = productImages.iterator();
        while(productImageIterator.hasNext())
        {
            String imagePath = productImageIterator.next().getPath();
            OssUtil.deleteFile(imagePath);
        }

        // step2 删除产品价格 product_price
        productPriceRepository.deleteByProductId(productId);

        // step3 删除产品属性 product_attribute
        productAttributeRepository.deleteByProductId(productId);
        // step4 删除竞价参数 bid_parameter（如果是竞价广告位）
        if (product.getPriceType() == 1)
        {
            bidParameterRepository.deleteByProductId(productId);
        }
        // step5 删除产品基本信息
        productRepository.delete(product);
    }

    @Override
    public List<Product> findByPositionId(Integer positionId) {
        return productRepository.findByPositionId(positionId);
    }

    @Override
    public List<Product> findByAccountIdAndNumber(Integer accountId, String number) {
        return productRepository.findByAccountIdAndNumber(accountId, number);
    }

    @Override
    public Product save2(Product product) {
        return productRepository.save(product);
    }
}
