/*
 *  *******************************************************************************
 *  Copyright (c) 2023 BMW AG
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 *    See the NOTICE file(s) distributed with this work for additional
 *    information regarding copyright ownership.
 *
 *    This program and the accompanying materials are made available under the
 *    terms of the Apache License, Version 2.0 which is available at
 *    https://www.apache.org/licenses/LICENSE-2.0.
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 *
 *    SPDX-License-Identifier: Apache-2.0
 *    ********************************************************************************
 */

package org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.services.impl;

import eclipse.tractusx.demand_capacity_mgmt_specification.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.entities.*;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.entities.enums.CapacityGroupStatus;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.exceptions.type.BadRequestException;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.exceptions.type.NotFoundException;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.repositories.CapacityGroupRepository;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.repositories.LinkDemandRepository;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.services.CapacityGroupService;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.services.CompanyService;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.services.DemandCategoryService;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.services.UnityOfMeasureService;
import org.eclipse.tractusx.demandcapacitymgmt.demandcapacitymgmtbackend.utils.UUIDUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
@Slf4j
public class CapacityGroupServiceImpl implements CapacityGroupService {

    private final CompanyService companyService;

    private final UnityOfMeasureService unityOfMeasureService;

    private final CapacityGroupRepository capacityGroupRepository;

    private final LinkDemandRepository linkDemandRepository;

    private final DemandCategoryService demandCategoryService;

    @Override
    public CapacityGroupResponse createCapacityGroup(CapacityGroupRequest capacityGroupRequest) {
        validateRequestFields(capacityGroupRequest);

        CapacityGroupEntity capacityGroupEntity = enrichCapacityGroup(capacityGroupRequest);

        capacityGroupEntity = capacityGroupRepository.save(capacityGroupEntity);

        return convertCapacityGroupDto(capacityGroupEntity);
    }

    @Override
    public CapacityGroupResponse getCapacityGroupById(String capacityGroupId) {
        CapacityGroupEntity capacityGroupEntity = getCapacityGroupEntity(capacityGroupId);
        return convertCapacityGroupDto(capacityGroupEntity);
    }

    @Override
    public List<CapacityGroupEntity> getAllByStatus(CapacityGroupStatus status) {
        return capacityGroupRepository.findAllByStatus(status);
    }

    @Override
    public List<CapacityGroupDefaultViewResponse> getAll() {
        List<CapacityGroupEntity> capacityGroupEntityList = capacityGroupRepository.findAll();
        return convertCapacityGroupEntity(capacityGroupEntityList);
    }

    private CapacityGroupEntity getCapacityGroupEntity(String capacityGroupId) {
        UUIDUtil.checkValidUUID(capacityGroupId);
        UUID uuid = UUIDUtil.generateUUIDFromString(capacityGroupId);
        Optional<CapacityGroupEntity> capacityGroup = capacityGroupRepository.findById(uuid);

        if (capacityGroup.isEmpty()) {
            throw new NotFoundException(
                404,
                "The capacity group provided was not found",
                new ArrayList<>(List.of("UUID provided : " + uuid))
            );
        }

        return capacityGroup.get();
    }

    private void validateRequestFields(CapacityGroupRequest capacityGroupRequest) {
        if (!UUIDUtil.checkValidUUID(capacityGroupRequest.getCustomer())) {
            throw new BadRequestException(
                400,
                "Not a valid customer ID",
                new ArrayList<>(List.of(capacityGroupRequest.getCustomer()))
            );
        }

        if (!UUIDUtil.checkValidUUID(capacityGroupRequest.getSupplier())) {
            throw new BadRequestException(
                400,
                "Not a valid supplier ID",
                new ArrayList<>(List.of(capacityGroupRequest.getSupplier()))
            );
        }

    }

    private CapacityGroupEntity enrichCapacityGroup(CapacityGroupRequest capacityGroupRequest) {
        UUID capacityGroupId = UUID.randomUUID();
        AtomicReference<String> materialNumberCustomer = new AtomicReference<>("");
        AtomicReference<String> materialDescriptionCustomer = new AtomicReference<>("");

        CompanyEntity supplier = companyService.getCompanyById(
            UUIDUtil.generateUUIDFromString(capacityGroupRequest.getSupplier())
        );

        CompanyEntity customer = companyService.getCompanyById(
            UUIDUtil.generateUUIDFromString(capacityGroupRequest.getSupplier())
        );
        return null;
    }

    private CapacityTimeSeries enrichCapacityTimeSeries(
        LocalDateTime calendarWeek,
        Double actualCapacity,
        Double maximumCapacity
    ) {
        return CapacityTimeSeries
            .builder()
            .id(UUID.randomUUID())
            .calendarWeek(calendarWeek)
            .actualCapacity(actualCapacity)
            .maximumCapacity(maximumCapacity)
            .build();
    }

    private CapacityGroupResponse convertCapacityGroupDto(CapacityGroupEntity capacityGroupEntity) {
        CapacityGroupResponse responseDto = new CapacityGroupResponse();

        CompanyDto customer = companyService.convertEntityToDto(capacityGroupEntity.getCustomerId());
        CompanyDto supplier = companyService.convertEntityToDto(capacityGroupEntity.getSupplierId());
        UnitMeasure unitMeasure = enrichUnitMeasure(capacityGroupEntity.getUnitMeasure());

        responseDto.setCustomer(customer);
        responseDto.setSupplier(supplier);
        responseDto.setUnitOfMeasure(unitMeasure);
        responseDto.setChangeAt(capacityGroupEntity.getChangedAt().toString());
        responseDto.setName(capacityGroupEntity.getName());
        responseDto.setWeekBasedCapacityGroupId(capacityGroupEntity.getCapacityGroupId().toString());
        responseDto.setCapacityGroupId(capacityGroupEntity.getId().toString());

        List<CapacityRequest> capacityRequests = capacityGroupEntity
            .getCapacityTimeSeries()
            .stream()
            .map(this::convertCapacityTimeSeries)
            .toList();

        responseDto.setCapacities(capacityRequests);

        List<LinkedDemandSeriesResponse> linkedDemandSeriesResponses = capacityGroupEntity
            .getLinkedDemandSeries()
            .stream()
            .map(this::convertLinkedDemandSeries)
            .toList();
        responseDto.setLinkedDemandSeries(linkedDemandSeriesResponses);

        List<CompanyDto> companyDtoList = capacityGroupEntity
            .getSupplierLocation()
            .stream()
            .map(this::convertString)
            .toList();

        responseDto.setSupplierLocations(companyDtoList);

        return responseDto;
    }

    private UnitMeasure enrichUnitMeasure(UnitMeasureEntity unitMeasureEntity) {
        UnitMeasure unitMeasure = new UnitMeasure();

        unitMeasure.setId(String.valueOf(unitMeasureEntity.getId()));
        unitMeasure.setDimension(unitMeasureEntity.getDimension());
        unitMeasure.setUnCode(unitMeasureEntity.getUnCode());
        unitMeasure.setDescription(unitMeasureEntity.getDescription());
        unitMeasure.setDescriptionGerman(unitMeasureEntity.getDescriptionGerman());
        unitMeasure.setUnSymbol(unitMeasureEntity.getUnSymbol());
        unitMeasure.setCxSymbol(unitMeasureEntity.getCxSymbol());

        return unitMeasure;
    }

    private CapacityRequest convertCapacityTimeSeries(CapacityTimeSeries capacityTimeSeries) {
        CapacityRequest capacityRequest = new CapacityRequest();

        capacityRequest.setActualCapacity(BigDecimal.valueOf(capacityTimeSeries.getActualCapacity()));
        capacityRequest.setMaximumCapacity(BigDecimal.valueOf(capacityTimeSeries.getMaximumCapacity()));
        capacityRequest.setCalendarWeek(capacityTimeSeries.getCalendarWeek().toString());

        return capacityRequest;
    }

    private LinkedDemandSeriesResponse convertLinkedDemandSeries(LinkedDemandSeries linkedDemandSeries) {
        LinkedDemandSeriesResponse linkedDemandSeriesResponse = new LinkedDemandSeriesResponse();

        linkedDemandSeriesResponse.setMaterialNumberCustomer(linkedDemandSeries.getMaterialNumberCustomer());
        linkedDemandSeriesResponse.setMaterialNumberSupplier(linkedDemandSeries.getMaterialNumberSupplier());

        CompanyDto customer = companyService.convertEntityToDto(linkedDemandSeries.getCustomerId());
        linkedDemandSeriesResponse.setCustomerLocation(customer);

        DemandCategoryResponse demand = convertDemandCategoryEntity(linkedDemandSeries.getDemandCategory());
        linkedDemandSeriesResponse.setDemandCategory(demand);

        return linkedDemandSeriesResponse;
    }

    private DemandCategoryResponse convertDemandCategoryEntity(DemandCategoryEntity demandCategoryEntity) {
        DemandCategoryResponse response = new DemandCategoryResponse();

        response.setId(demandCategoryEntity.getId().toString());
        response.setDemandCategoryCode(demandCategoryEntity.getDemandCategoryCode());
        response.setDemandCategoryName(demandCategoryEntity.getDemandCategoryName());

        return response;
    }

    private CompanyDto convertString(String supplier) {
        CompanyEntity entity = companyService.getCompanyById(UUID.fromString(supplier));

        return companyService.convertEntityToDto(entity);
    }

    private List<CapacityGroupDefaultViewResponse> convertCapacityGroupEntity(
        List<CapacityGroupEntity> capacityGroupEntityList
    ) {
        List<CapacityGroupDefaultViewResponse> capacityGroupList = new ArrayList<>();

        for (CapacityGroupEntity entity : capacityGroupEntityList) {
            CapacityGroupDefaultViewResponse response = new CapacityGroupDefaultViewResponse();

            response.setName(entity.getName());
            response.setStatus(entity.getStatus().toString());
            response.setSupplierBNPL(entity.getSupplierId().getBpn());
            response.setCustomerName(entity.getCustomerId().getCompanyName());
            response.setCustomerBPNL(entity.getCustomerId().getBpn());
            response.setInternalId(entity.getId().toString());
            response.setNumberOfMaterials(new BigDecimal(entity.getCapacityTimeSeries().size()));
            //response.setFavoritedBy();
            //response.setCatXUuid();

            capacityGroupList.add(response);
        }

        return capacityGroupList;
    }
}
