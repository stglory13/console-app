package st.flyapp.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import st.flyapp.api.dto.LedgerDetailDto;
import st.flyapp.model.Ledger;

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
