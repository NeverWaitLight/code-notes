package com.example.videodemo.repository;

import com.example.videodemo.entity.HlsPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HlsPackageRepository extends JpaRepository<HlsPackage, Long> {
}
