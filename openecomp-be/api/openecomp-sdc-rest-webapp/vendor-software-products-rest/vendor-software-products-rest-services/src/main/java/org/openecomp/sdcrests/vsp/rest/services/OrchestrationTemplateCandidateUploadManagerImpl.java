/*
 * -
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.openecomp.sdcrests.vsp.rest.services;

import static org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier.couldNotCreateLock;
import static org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier.couldNotFindLock;
import static org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier.couldNotUpdateLock;
import static org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier.uploadAlreadyFinished;
import static org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier.vspUploadAlreadyInProgress;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.openecomp.sdc.common.errors.CoreException;
import org.openecomp.sdc.vendorsoftwareproduct.VendorSoftwareProductManager;
import org.openecomp.sdc.vendorsoftwareproduct.VspManagerFactory;
import org.openecomp.sdc.vendorsoftwareproduct.dao.VspUploadStatusRecordDao;
import org.openecomp.sdc.vendorsoftwareproduct.dao.type.VspDetails;
import org.openecomp.sdc.vendorsoftwareproduct.dao.type.VspUploadStatus;
import org.openecomp.sdc.vendorsoftwareproduct.dao.type.VspUploadStatusRecord;
import org.openecomp.sdc.versioning.dao.types.Version;
import org.openecomp.sdcrests.vendorsoftwareproducts.types.VspUploadStatusDto;
import org.openecomp.sdcrests.vsp.rest.exception.OrchestrationTemplateCandidateUploadManagerExceptionSupplier;
import org.openecomp.sdcrests.vsp.rest.mapping.VspUploadStatusRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Manages the package upload process status.
 */
@Service
public class OrchestrationTemplateCandidateUploadManagerImpl implements OrchestrationTemplateCandidateUploadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationTemplateCandidateUploadManagerImpl.class);

    private final VspUploadStatusRecordDao uploadManagerDao;
    private final VspUploadStatusRecordMapper vspUploadStatusRecordMapper;
    private final VendorSoftwareProductManager vendorSoftwareProductManager;
    private final Lock startUploadLock;

    @Autowired
    public OrchestrationTemplateCandidateUploadManagerImpl(
        @Qualifier("vsp-upload-status-record-dao-impl") final VspUploadStatusRecordDao uploadManagerDao) {

        this.uploadManagerDao = uploadManagerDao;
        this.vendorSoftwareProductManager = VspManagerFactory.getInstance().createInterface();
        this.vspUploadStatusRecordMapper = new VspUploadStatusRecordMapper();
        startUploadLock = new ReentrantLock();
    }

    //for tests purpose
    OrchestrationTemplateCandidateUploadManagerImpl(final VspUploadStatusRecordDao uploadManagerDao,
                                                    final VendorSoftwareProductManager vendorSoftwareProductManager) {
        this.uploadManagerDao = uploadManagerDao;
        this.vendorSoftwareProductManager = vendorSoftwareProductManager;
        this.vspUploadStatusRecordMapper = new VspUploadStatusRecordMapper();
        startUploadLock = new ReentrantLock();
    }

    @Override
    public VspUploadStatusDto putUploadInProgress(final String vspId, final String vspVersionId, final String user) {
        checkVspExists(vspId, vspVersionId);
        LOGGER.debug("Start uploading for VSP id '{}', version '{}', triggered by user '{}'", vspId, vspVersionId, user);

        final VspUploadStatusRecord vspUploadStatusRecord;
        startUploadLock.lock();
        try {
            final List<VspUploadStatusRecord> uploadInProgressList = uploadManagerDao.findAllInProgress(vspId, vspVersionId);
            if (!uploadInProgressList.isEmpty()) {
                throw vspUploadAlreadyInProgress(vspId, vspVersionId).get();
            }

            vspUploadStatusRecord = new VspUploadStatusRecord();
            vspUploadStatusRecord.setStatus(VspUploadStatus.UPLOADING);
            vspUploadStatusRecord.setVspId(vspId);
            vspUploadStatusRecord.setVspVersionId(vspVersionId);
            vspUploadStatusRecord.setLockId(UUID.randomUUID());
            vspUploadStatusRecord.setCreated(new Date());

            uploadManagerDao.create(vspUploadStatusRecord);
            LOGGER.debug("Upload lock '{}' created for VSP id '{}', version '{}'", vspUploadStatusRecord.getLockId(), vspId, vspVersionId);
        } catch (final CoreException e) {
            throw e;
        } catch (final Exception e) {
            throw couldNotCreateLock(vspId, vspVersionId, e).get();
        } finally {
            startUploadLock.unlock();
        }

        return vspUploadStatusRecordMapper.applyMapping(vspUploadStatusRecord, VspUploadStatusDto.class);
    }

    @Override
    public VspUploadStatusDto putUploadAsFinished(final String vspId, final String vspVersionId, final UUID lockId, final VspUploadStatus completionStatus,
                                                  final String user) {

        if (!completionStatus.isCompleteStatus()) {
            throw OrchestrationTemplateCandidateUploadManagerExceptionSupplier.invalidCompleteStatus(completionStatus).get();
        }
        final Optional<VspUploadStatusRecord> vspUploadStatusOptional =
            uploadManagerDao.findByVspIdAndVersionIdAndLockId(vspId, vspVersionId, lockId);
        if (vspUploadStatusOptional.isEmpty()) {
            throw couldNotFindLock(lockId, vspId, vspVersionId).get();
        }
        final VspUploadStatusRecord vspUploadStatusRecord = vspUploadStatusOptional.get();
        if (vspUploadStatusRecord.getIsComplete()) {
            throw uploadAlreadyFinished(lockId, vspId, vspVersionId).get();
        }
        LOGGER.debug("Finishing the upload for VSP id '{}', version '{}', lock '{}', triggered by user '{}'",
            vspUploadStatusRecord.getVspId(), vspUploadStatusRecord.getVspVersionId(), vspUploadStatusRecord.getLockId(), user);
        vspUploadStatusRecord.setStatus(completionStatus);
        vspUploadStatusRecord.setUpdated(new Date());
        vspUploadStatusRecord.setIsComplete(true);

        try {
            uploadManagerDao.update(vspUploadStatusRecord);
            LOGGER.debug("Upload complete for VSP '{}', version '{}', lock '{}'",
                vspUploadStatusRecord.getLockId(), vspUploadStatusRecord.getVspId(), vspUploadStatusRecord.getVspVersionId());
        } catch (final Exception e) {
            throw couldNotUpdateLock(vspUploadStatusRecord.getLockId(), vspUploadStatusRecord.getVspId(), vspUploadStatusRecord.getVspVersionId(), e)
                .get();
        }

        return vspUploadStatusRecordMapper.applyMapping(vspUploadStatusRecord, VspUploadStatusDto.class);
    }

    private void checkVspExists(final String vspId, final String vspVersionId) {
        final VspDetails vspDetails = vendorSoftwareProductManager.getVsp(vspId, new Version(vspVersionId));
        if (vspDetails == null) {
            throw OrchestrationTemplateCandidateUploadManagerExceptionSupplier.vspNotFound(vspId, vspVersionId).get();
        }
    }

    @Override
    public Optional<VspUploadStatusDto> findLatestStatus(final String vspId, final String vspVersionId, final String user) {
        checkVspExists(vspId, vspVersionId);

        final Optional<VspUploadStatusRecord> vspUploadStatus = uploadManagerDao.findLatest(vspId, vspVersionId);
        if (vspUploadStatus.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(vspUploadStatusRecordMapper.applyMapping(vspUploadStatus.get(), VspUploadStatusDto.class));
    }

}
