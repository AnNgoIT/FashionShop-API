package fit.tlcn.fashionshopbe.service.impl;

import fit.tlcn.fashionshopbe.constant.Status;
import fit.tlcn.fashionshopbe.dto.GenericResponse;
import fit.tlcn.fashionshopbe.dto.OrderItemResponse;
import fit.tlcn.fashionshopbe.entity.*;
import fit.tlcn.fashionshopbe.repository.*;
import fit.tlcn.fashionshopbe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ProductItemRepository productItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Override
    public ResponseEntity<GenericResponse> getOrdersByStatus(Status status) {
        try {
            List<Order> orderList = orderRepository.findAllByStatusCheck(status);
            Map<String, Object> map = new HashMap<>();
            map.put("content", orderList);
            map.put("totalElements", orderList.size());

            return ResponseEntity.status(HttpStatus.OK).body(
                    GenericResponse.builder()
                            .success(true)
                            .message("Successful")
                            .result(map)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GenericResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .result("Internal server error")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<GenericResponse> getOrderByOrderId(Integer orderId) {
        try {
            Optional<Order> orderOptional = orderRepository.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("OrderId " + orderId + " does not exist")
                                .result("Not found")
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .build()
                );
            }

            List<OrderItem> orderItemList = orderItemRepository.findAllByOrder(orderOptional.get());

            List<OrderItemResponse> orderItemResponseList = new ArrayList<>();
            for (OrderItem orderItem : orderItemList) {
                OrderItemResponse orderItemResponse = new OrderItemResponse();
                orderItemResponse.setOrderItemId(orderItem.getOrderItemId());
                orderItemResponse.setProductItemId(orderItem.getProductItem().getProductItemId());
                orderItemResponse.setProductName(orderItem.getProductItem().getParent().getName());
                orderItemResponse.setImage(orderItem.getProductItem().getImage());
                List<String> styleValueNames = new ArrayList<>();
                for (StyleValue styleValue : orderItem.getProductItem().getStyleValues()) {
                    styleValueNames.add(styleValue.getName());
                }
                orderItemResponse.setStyleValues(styleValueNames);
                orderItemResponse.setQuantity(orderItem.getQuantity());
                orderItemResponse.setProductPrice(orderItem.getProductItem().getPrice());
                orderItemResponse.setProductPromotionalPrice(orderItem.getProductItem().getPromotionalPrice());
                orderItemResponse.setAmount(orderItem.getAmount());

                orderItemResponseList.add(orderItemResponse);
            }

            Order order = orderOptional.get();
            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            map.put("orderItems", orderItemResponseList);
            map.put("totalOrderItems", orderItemResponseList.size());

            return ResponseEntity.status(HttpStatus.OK).body(
                    GenericResponse.builder()
                            .success(true)
                            .message("Get this order successfully")
                            .result(map)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GenericResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .result("Internal server error")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<GenericResponse> updateOrderStatusToProcessing(Integer orderId) {
        try {
            Optional<Order> orderOptional = orderRepository.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("OrderId " + orderId + " does not exist")
                                .result("Not found")
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .build()
                );
            }

            Order order = orderOptional.get();

            if (order.getStatus() != Status.NOT_PROCESSED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("Only orders with the status NOT_PROCESSED can be updated to processing")
                                .result("Bad request")
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
            }

            List<OrderItem> orderItemList = orderItemRepository.findAllByOrder(order);

            for (OrderItem orderItem : orderItemList) {
                ProductItem productItem = orderItem.getProductItem();
                productItem.setSold(productItem.getSold() + orderItem.getQuantity());
                productItemRepository.save(productItem);

                Product product = productItem.getParent();
                product.setTotalSold(product.getTotalSold() + orderItem.getQuantity());
                productRepository.save(product);
            }

            order.setStatus(Status.PROCESSING);
            orderRepository.save(order);

            return ResponseEntity.status(HttpStatus.OK).body(
                    GenericResponse.builder()
                            .success(true)
                            .message("Update order's status to processing successfully")
                            .result(order)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GenericResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .result("Internal server error")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<GenericResponse> updateOrderStatusToShipping(Integer orderId, String address) {
        try {
            Optional<Order> orderOptional = orderRepository.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("OrderId " + orderId + " does not exist")
                                .result("Not found")
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .build()
                );
            }

            Order order = orderOptional.get();

            if (order.getStatus() != Status.PROCESSING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("Only orders with the status PROCESSING can be updated to shipping")
                                .result("Bad request")
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
            }

            //Lấy ra province của order để so sánh với address (param)
            String[] parts = order.getAddress().split("-");
            String province = parts[1].trim();

            if (!province.equals(address)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("Invalid address")
                                .result("Bad request")
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
            }

            String roleName = "SHIPPER";
            List<User> shipperList = userRepository.findAllByRole_NameAndAddress(roleName, address);

            if (shipperList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        GenericResponse.builder()
                                .success(false)
                                .message("There are no shippers in this area")
                                .result("Bad request")
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
            }

            order.setStatus(Status.SHIPPING);
            orderRepository.save(order);

            Random random = new Random();
            int randomIndex = random.nextInt(shipperList.size());
            User selectedShipper = shipperList.get(randomIndex);

            Delivery delivery = new Delivery();
            delivery.setOrder(order);
            delivery.setShipper(selectedShipper);
            deliveryRepository.save(delivery);

            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            map.put("delivery", delivery);

            return ResponseEntity.status(HttpStatus.OK).body(
                    GenericResponse.builder()
                            .success(true)
                            .message("Update order's status to shipping successfully")
                            .result(map)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GenericResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .result("Internal server error")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<GenericResponse> getAllOrders() {
        try {
            List<Order> orderList = orderRepository. findAllByAddressContainingOrderByUpdatedAtDesc("");
            Map<String, Object> map = new HashMap<>();
            map.put("content", orderList);
            map.put("totalElements", orderList.size());

            return ResponseEntity.status(HttpStatus.OK).body(
                    GenericResponse.builder()
                            .success(true)
                            .message("Successful")
                            .result(map)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GenericResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .result("Internal server error")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }
}
