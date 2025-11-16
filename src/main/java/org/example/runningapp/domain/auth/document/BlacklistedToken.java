package org.example.runningapp.domain.auth.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@Document(collection = "blacklisted_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {

    @Id
    private String id;

    private String token;

    @Indexed(name = "expire_at_ttl_index", expireAfterSeconds = 0)
    private Date expireAt;
}