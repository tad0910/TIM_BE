package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.entity.TuitionInstallmentConfig;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.repository.TuitionRouteRepository;
import com.tim.appTim.repository.ProgramsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TuitionRouteServiceTest {

    @Mock
    private TuitionRouteRepository tuitionRouteRepository;

    @Mock
    private ProgramsRepository programsRepository;

    @InjectMocks
    private TuitionRouteService tuitionRouteService;

    private TuitionRoute tuitionRoute;
    private Programs program;
    private TuitionRouteDTO tuitionRouteDTO;
    private TuitionInstallmentConfig installmentConfig;
    private InstallmentConfigDTO installmentConfigDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        program = new Programs();
        program.setId(1);
        program.setName("Test Program");
        program.setDescription("Test Description");

        tuitionRoute = new TuitionRoute();
        tuitionRoute.setId(1L);
        tuitionRoute.setName("Test Route");
        tuitionRoute.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
        tuitionRoute.setAdmissionFee(new BigDecimal("1000000"));
        tuitionRoute.setFirstMonthFee(new BigDecimal("5000000"));
        tuitionRoute.setTotalListedFee(new BigDecimal("20000000"));
        tuitionRoute.setNumberOfInstallments(4);
        tuitionRoute.setFrequency(1);
        tuitionRoute.setDescription("Test Description");
        tuitionRoute.setProgram(program);
        tuitionRoute.setInstallmentConfigs(new ArrayList<>());

        installmentConfig = new TuitionInstallmentConfig();
        installmentConfig.setId(1L);
        installmentConfig.setInstallmentNumber(1);
        installmentConfig.setBaseAmount(new BigDecimal("5000000"));
        installmentConfig.setDaysFromPrevious(0);
        installmentConfig.setTuitionRoute(tuitionRoute);

        tuitionRoute.getInstallmentConfigs().add(installmentConfig);

        tuitionRouteDTO = new TuitionRouteDTO();
        tuitionRouteDTO.setId(1L);
        tuitionRouteDTO.setProgramId(1);
        tuitionRouteDTO.setName("Test Route");
        tuitionRouteDTO.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
        tuitionRouteDTO.setAdmissionFee(new BigDecimal("1000000"));
        tuitionRouteDTO.setFirstMonthFee(new BigDecimal("5000000"));
        tuitionRouteDTO.setTotalListedFee(new BigDecimal("20000000"));
        tuitionRouteDTO.setNumberOfInstallments(4);
        tuitionRouteDTO.setFrequency(1);
        tuitionRouteDTO.setDescription("Test Description");

        installmentConfigDTO = new InstallmentConfigDTO();
        installmentConfigDTO.setInstallmentNumber(1);
        installmentConfigDTO.setBaseAmount(new BigDecimal("5000000"));
        installmentConfigDTO.setDaysFromPrevious(0);
    }

    // --- getAllRoutes ---

    @Test
    void getAllRoutes_WithData_ShouldReturnPage() {
        // Arrange
        Page<TuitionRoute> routePage = new PageImpl<>(List.of(tuitionRoute));
        when(tuitionRouteRepository.findAll(pageable)).thenReturn(routePage);

        // Act
        Page<TuitionRouteDTO> result = tuitionRouteService.getAllRoutes(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Route");
        verify(tuitionRouteRepository).findAll(pageable);
    }

    @Test
    void getAllRoutes_EmptyPage_ShouldReturnEmptyPage() {
        // Arrange
        Page<TuitionRoute> emptyPage = new PageImpl<>(Collections.emptyList());
        when(tuitionRouteRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<TuitionRouteDTO> result = tuitionRouteService.getAllRoutes(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(tuitionRouteRepository).findAll(pageable);
    }

    @Test
    void getAllRoutes_ShouldNotIncludeConfigs() {
        // Arrange
        Page<TuitionRoute> routePage = new PageImpl<>(List.of(tuitionRoute));
        when(tuitionRouteRepository.findAll(pageable)).thenReturn(routePage);

        // Act
        Page<TuitionRouteDTO> result = tuitionRouteService.getAllRoutes(pageable);

        // Assert
        assertThat(result.getContent().get(0).getInstallmentConfigs()).isNull();
    }

    // --- getRouteById ---

    @Test
    void getRouteById_WhenExists_ShouldReturnDTO() {
        // Arrange
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Route");
        assertThat(result.getProgramId()).isEqualTo(1);
        assertThat(result.getInstallmentConfigs()).isNotNull();
        assertThat(result.getInstallmentConfigs()).hasSize(1);
        verify(tuitionRouteRepository).findById(1L);
    }

    @Test
    void getRouteById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(tuitionRouteRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.getRouteById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy lộ trình học phí với ID: 999");
        verify(tuitionRouteRepository).findById(999L);
    }

    @Test
    void getRouteById_WithConfigs_ShouldIncludeConfigs() {
        // Arrange
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result.getInstallmentConfigs()).isNotNull();
        assertThat(result.getInstallmentConfigs()).hasSize(1);
        assertThat(result.getInstallmentConfigs().get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.getInstallmentConfigs().get(0).getBaseAmount())
                .isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    void getRouteById_WithoutConfigs_ShouldReturnEmptyConfigsList() {
        // Arrange
        tuitionRoute.setInstallmentConfigs(new ArrayList<>());
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result.getInstallmentConfigs()).isNull();
    }

    @Test
    void getRouteById_WithNullProgram_ShouldHandleGracefully() {
        // Arrange
        tuitionRoute.setProgram(null);
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result.getProgramId()).isNull();
    }

    // --- createRoute ---

    @Test
    void createRoute_WithoutConfigs_ShouldCreateRoute() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(4);
        dto.setInstallmentConfigs(null);

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            route.setId(1L);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.createRoute(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Route");
        assertThat(result.getProgramId()).isEqualTo(1);
        verify(programsRepository).findById(1);
        verify(tuitionRouteRepository).existsByProgram_Id(1);
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    @Test
    void createRoute_WithConfigs_ShouldCreateRouteAndConfigs() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            route.setId(1L);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.createRoute(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    @Test
    void createRoute_WhenProgramNotFound_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(999);
        dto.setName("New Route");

        when(programsRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.createRoute(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chương trình học không tồn tại với ID: 999");
        verify(programsRepository).findById(999);
        verify(tuitionRouteRepository, never()).save(any(TuitionRoute.class));
    }

    @Test
    void createRoute_WhenRouteAlreadyExists_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.createRoute(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chương trình đã có lộ trình học phí, không thể tạo thêm");
        verify(programsRepository).findById(1);
        verify(tuitionRouteRepository).existsByProgram_Id(1);
        verify(tuitionRouteRepository, never()).save(any(TuitionRoute.class));
    }

    @Test
    void createRoute_WithEmptyConfigsList_ShouldNotValidate() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setInstallmentConfigs(Collections.emptyList());

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            route.setId(1L);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.createRoute(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    // --- updateRoute ---

    @Test
    void updateRoute_WhenExists_ShouldUpdateRoute() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setDescription("Updated Description");
        dto.setInstallmentConfigs(null);

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.updateRoute(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Route");
        verify(tuitionRouteRepository).findById(1L);
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    @Test
    void updateRoute_WhenNotExists_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");

        when(tuitionRouteRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.updateRoute(999L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy lộ trình để cập nhật");
        verify(tuitionRouteRepository).findById(999L);
        verify(tuitionRouteRepository, never()).save(any(TuitionRoute.class));
    }

    @Test
    void updateRoute_WithConfigs_ShouldUpdateConfigs() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.updateRoute(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    @Test
    void updateRoute_WithNullConfigs_ShouldNotUpdateConfigs() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setInstallmentConfigs(null);

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));
        when(tuitionRouteRepository.save(any(TuitionRoute.class))).thenAnswer(invocation -> {
            TuitionRoute route = invocation.getArgument(0);
            return route;
        });

        // Act
        TuitionRouteDTO result = tuitionRouteService.updateRoute(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        verify(tuitionRouteRepository).save(any(TuitionRoute.class));
    }

    // --- deleteRoute ---

    @Test
    void deleteRoute_WhenExists_ShouldDeleteRoute() {
        // Arrange
        when(tuitionRouteRepository.existsById(1L)).thenReturn(true);
        doNothing().when(tuitionRouteRepository).deleteById(1L);

        // Act
        tuitionRouteService.deleteRoute(1L);

        // Assert
        verify(tuitionRouteRepository).existsById(1L);
        verify(tuitionRouteRepository).deleteById(1L);
    }

    @Test
    void deleteRoute_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(tuitionRouteRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.deleteRoute(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy lộ trình để xóa");
        verify(tuitionRouteRepository).existsById(999L);
        verify(tuitionRouteRepository, never()).deleteById(anyLong());
    }

    // --- validateInstallmentConfigs (tested through createRoute and updateRoute) ---

    @Test
    void createRoute_WhenConfigCountMismatch_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(3); // Expects 3, but only 2 configs provided

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.createRoute(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số lượng cấu hình (2) không khớp với số kỳ (3)");
    }

    @Test
    void createRoute_WhenConfigSumMismatch_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("5000000")); // Sum = 15000000, not 20000000
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.createRoute(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tổng tiền các kỳ");
    }

    @Test
    void createRoute_WhenInstallmentNumbersNotSequential_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("New Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(3); // Should be 2, not 3
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(programsRepository.findById(1)).thenReturn(Optional.of(program));
        when(tuitionRouteRepository.existsByProgram_Id(1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.createRoute(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số kỳ phải liên tục từ 1 đến 2");
    }

    @Test
    void updateRoute_WhenConfigCountMismatch_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(3); // Expects 3, but only 2 configs provided

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.updateRoute(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số lượng cấu hình (2) không khớp với số kỳ (3)");
    }

    @Test
    void updateRoute_WhenConfigSumMismatch_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("5000000")); // Sum = 15000000, not 20000000
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.updateRoute(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tổng tiền các kỳ");
    }

    @Test
    void updateRoute_WhenInstallmentNumbersNotSequential_ShouldThrowException() {
        // Arrange
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setName("Updated Route");
        dto.setTotalListedFee(new BigDecimal("20000000"));
        dto.setNumberOfInstallments(2);

        InstallmentConfigDTO config1 = new InstallmentConfigDTO();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("10000000"));
        config1.setDaysFromPrevious(0);

        InstallmentConfigDTO config2 = new InstallmentConfigDTO();
        config2.setInstallmentNumber(3); // Should be 2, not 3
        config2.setBaseAmount(new BigDecimal("10000000"));
        config2.setDaysFromPrevious(30);

        dto.setInstallmentConfigs(List.of(config1, config2));

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act & Assert
        assertThatThrownBy(() -> tuitionRouteService.updateRoute(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số kỳ phải liên tục từ 1 đến 2");
    }

    // --- convertToDTO (tested through public methods) ---

    @Test
    void convertToDTO_WithAllFields_ShouldMapCorrectly() {
        // Arrange
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result.getId()).isEqualTo(tuitionRoute.getId());
        assertThat(result.getName()).isEqualTo(tuitionRoute.getName());
        assertThat(result.getType()).isEqualTo(tuitionRoute.getType());
        assertThat(result.getAdmissionFee()).isEqualByComparingTo(tuitionRoute.getAdmissionFee());
        assertThat(result.getFirstMonthFee()).isEqualByComparingTo(tuitionRoute.getFirstMonthFee());
        assertThat(result.getTotalListedFee()).isEqualByComparingTo(tuitionRoute.getTotalListedFee());
        assertThat(result.getNumberOfInstallments()).isEqualTo(tuitionRoute.getNumberOfInstallments());
        assertThat(result.getFrequency()).isEqualTo(tuitionRoute.getFrequency());
        assertThat(result.getDescription()).isEqualTo(tuitionRoute.getDescription());
        assertThat(result.getProgramId()).isEqualTo(program.getId());
    }

    @Test
    void convertToDTO_WithMultipleConfigs_ShouldMapAllConfigs() {
        // Arrange
        TuitionInstallmentConfig config2 = new TuitionInstallmentConfig();
        config2.setId(2L);
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("5000000"));
        config2.setDaysFromPrevious(30);
        config2.setTuitionRoute(tuitionRoute);

        tuitionRoute.getInstallmentConfigs().add(config2);

        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(tuitionRoute));

        // Act
        TuitionRouteDTO result = tuitionRouteService.getRouteById(1L);

        // Assert
        assertThat(result.getInstallmentConfigs()).hasSize(2);
        assertThat(result.getInstallmentConfigs().get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.getInstallmentConfigs().get(1).getInstallmentNumber()).isEqualTo(2);
    }

    @Test
    void getAllRoutes_WithMultipleRoutes_ShouldReturnAll() {
        // Arrange
        TuitionRoute route2 = new TuitionRoute();
        route2.setId(2L);
        route2.setName("Route 2");
        route2.setType(TuitionRoute.TuitionRouteType.PART_TIME);
        route2.setProgram(program);

        Page<TuitionRoute> routePage = new PageImpl<>(List.of(tuitionRoute, route2));
        when(tuitionRouteRepository.findAll(pageable)).thenReturn(routePage);

        // Act
        Page<TuitionRouteDTO> result = tuitionRouteService.getAllRoutes(pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
    }
}


