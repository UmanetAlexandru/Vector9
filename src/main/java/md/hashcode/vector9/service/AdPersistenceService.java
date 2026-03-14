package md.hashcode.vector9.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import md.hashcode.vector9.jooq.tables.records.AdsRecord;
import md.hashcode.vector9.model.AdImageInput;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.ProcessedAdResult;
import md.hashcode.vector9.repository.AdImageRepository;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.OwnerRepository;
import md.hashcode.vector9.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdPersistenceService {

    private final OwnerRepository ownerRepository;
    private final AdRepository adRepository;
    private final AdImageRepository adImageRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionTemplate transactionTemplate;

    public AdPersistenceService(
            OwnerRepository ownerRepository,
            AdRepository adRepository,
            AdImageRepository adImageRepository,
            PriceHistoryRepository priceHistoryRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.ownerRepository = ownerRepository;
        this.adRepository = adRepository;
        this.adImageRepository = adImageRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ProcessedAdResult persist(ProcessedAdCommand command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.ad(), "command.ad");

        return transactionTemplate.execute(status -> persistInsideTransaction(command));
    }

    private ProcessedAdResult persistInsideTransaction(ProcessedAdCommand command) {
        LocalDateTime now = LocalDateTime.now();
        AdUpsertCommand ad = command.ad();
        Optional<AdsRecord> existing = adRepository.findById(ad.id());

        UUID ownerId = null;
        if (command.owner() != null) {
            ownerRepository.upsert(command.owner(), now);
            ownerId = command.owner().id();
        }
        UUID resolvedOwnerId = ownerId;

        adRepository.upsert(ad, resolvedOwnerId, now);

        boolean priceChanged = existing
                .map(current -> hasPriceChanged(current, ad))
                .orElse(false);
        if (priceChanged) {
            priceHistoryRepository.insertChange(
                    ad.id(),
                    existing.map(AdsRecord::getPriceValue).orElse(null),
                    ad.priceValue(),
                    ad.priceUnit(),
                    now
            );
        }

        List<AdImageInput> images = command.images() != null ? command.images() : List.of();
        int insertedImages = adImageRepository.insertMissing(ad.id(), images);
        boolean materiallyChanged = existing
                .map(current -> hasMaterialChanges(current, ad, resolvedOwnerId, insertedImages))
                .orElse(true);

        return new ProcessedAdResult(existing.isEmpty(), materiallyChanged, priceChanged, insertedImages);
    }

    private boolean hasPriceChanged(AdsRecord current, AdUpsertCommand updated) {
        return !samePrice(current.getPriceValue(), updated.priceValue())
                || !Objects.equals(current.getPriceUnit(), updated.priceUnit());
    }

    private boolean hasMaterialChanges(AdsRecord current, AdUpsertCommand updated, UUID ownerId, int insertedImages) {
        return !Objects.equals(current.getTitle(), updated.title())
                || !Objects.equals(current.getSubcategoryId(), updated.subcategoryId())
                || !samePrice(current.getPriceValue(), updated.priceValue())
                || !Objects.equals(current.getPriceUnit(), updated.priceUnit())
                || !Objects.equals(current.getPriceMeasurement(), updated.priceMeasurement())
                || !Objects.equals(current.getPriceMode(), updated.priceMode())
                || !samePrice(current.getPricePerMeter(), updated.pricePerMeter())
                || !samePrice(current.getOldPriceValue(), updated.oldPriceValue())
                || !Objects.equals(current.getBodyRo(), updated.bodyRo())
                || !Objects.equals(current.getBodyRu(), updated.bodyRu())
                || !Objects.equals(current.getAdState(), updated.adState())
                || !Objects.equals(current.getOfferTypeId(), updated.offerTypeId())
                || !Objects.equals(current.getOfferTypeValue(), updated.offerTypeValue())
                || !Objects.equals(current.getOfferTypeText(), updated.offerTypeText())
                || !Objects.equals(current.getOwnerId(), ownerId)
                || !Objects.equals(current.getTransportYear(), updated.transportYear())
                || !Objects.equals(current.getRealEstateType(), updated.realEstateType())
                || !Objects.equals(current.getStatus(), updated.status())
                || insertedImages > 0;
    }

    private boolean samePrice(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }
}