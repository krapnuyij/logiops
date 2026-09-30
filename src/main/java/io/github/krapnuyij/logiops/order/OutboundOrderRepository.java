package io.github.krapnuyij.logiops.order;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

  @Query("select distinct outboundOrder from OutboundOrder outboundOrder "
      + "join fetch outboundOrder.items item "
      + "join fetch item.product "
      + "where outboundOrder.id = :orderId")
  Optional<OutboundOrder> findDetailedById(@Param("orderId") long orderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select outboundOrder from OutboundOrder outboundOrder "
      + "where outboundOrder.id = :orderId")
  Optional<OutboundOrder> findByIdForUpdate(@Param("orderId") long orderId);
}
