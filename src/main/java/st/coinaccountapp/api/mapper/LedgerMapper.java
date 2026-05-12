package st.coinaccountapp.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.model.Ledger;

/**
 * MapStruct mapper z JPA entity {@link Ledger} na REST DTO {@link LedgerDetailDto}.
 * Implementáciu generuje MapStruct pri buildovaní, Spring ju injectne ako bean.
 */
@Mapper(componentModel = "spring")
public interface LedgerMapper {

    @Mapping(source = "id", target = "ledgerId")
    @Mapping(source = "fromAccount.guid", target = "fromAccountGuid")
    @Mapping(source = "toAccount.guid", target = "toAccountGuid")
    LedgerDetailDto toDto(Ledger ledger);
}
