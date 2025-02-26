package br.com.alelo.consumer.consumerpat.domain.service;

import br.com.alelo.consumer.consumerpat.domain.entity.Card;
import br.com.alelo.consumer.consumerpat.domain.entity.Extract;
import br.com.alelo.consumer.consumerpat.domain.service.exception.ApiException;
import br.com.alelo.consumer.consumerpat.domain.service.exception.Code;
import br.com.alelo.consumer.consumerpat.integration.respository.CardRepository;
import br.com.alelo.consumer.consumerpat.integration.respository.ExtractRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ExtractService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ExtractRepository extractRepository;

    @Transactional(rollbackFor = Throwable.class)
    public Extract buy(Extract extract) throws ApiException {
        try {
            Optional<Card> cardOut = cardRepository.findByCardCode(extract.getCards().stream().findFirst().get().getCardCode());

            if (cardOut.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, Code.INVALID_NOT_FOUND);
            }

            cardOut.get().setBalance(cardOut.get().getType().getCardContext().applyCashback(
                    cardOut.get().getBalance(),
                    extract.getValue()));

            if (cardOut.get().getBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, Code.INVALID_REFUND);
            }

            Card card = cardRepository.save(cardOut.get());
            extract.setCards(Set.of(card));

            return extractRepository.save(extract);
        } catch (ApiException e) {
            log.warn("m=buy, stage=warn, excption={}", e.getCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("m=buy, stage=error, excption={}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, Code.INVALID_EXCEPTION);
        }
    }

}
