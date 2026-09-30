package io.github.krapnuyij.logiops.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

  @Query("select movement from StockMovement movement "
      + "where (:productId is null or movement.product.id = :productId) "
      + "and (:outboundOrderId is null or movement.outboundOrderId = :outboundOrderId) "
      + "and (:type is null or movement.type = :type)")
  Page<StockMovement> findAllByFilters(
      @Param("productId") Long productId,
      @Param("outboundOrderId") Long outboundOrderId,
      @Param("type") StockMovementType type,
      Pageable pageable
  );
}
