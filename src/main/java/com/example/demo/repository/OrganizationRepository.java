package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Organization;
import com.example.demo.entity.OrganizationType;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Gợi ý khi gõ tên trường / công ty. Collation utf8mb4_unicode_ci của MySQL không phân biệt dấu
    // nên gõ "bach khoa" vẫn ra "Bách Khoa". Đã xác minh + khớp đầu tên đứng trước
    @Query("""
        SELECT o FROM Organization o
        WHERE o.type = :type AND o.name LIKE CONCAT('%', :keyword, '%')
        ORDER BY o.verified DESC,
                 CASE WHEN o.name LIKE CONCAT(:keyword, '%') THEN 0 ELSE 1 END,
                 o.name
    """)
    List<Organization> search(@Param("type") OrganizationType type, @Param("keyword") String keyword,
            Pageable pageable);

    // Tạo mới khi người dùng gõ tên chưa có: dùng lại nếu đã có đúng tên + thành phố (chống tạo trùng)
    @Query("""
        SELECT o FROM Organization o
        WHERE o.type = :type AND o.name = :name
          AND ((:city IS NULL AND o.city IS NULL) OR o.city = :city)
    """)
    List<Organization> findSame(@Param("type") OrganizationType type, @Param("name") String name,
            @Param("city") String city);

    Optional<Organization> findByIdAndType(Long id, OrganizationType type);
}
