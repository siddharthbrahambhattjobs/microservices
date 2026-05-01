package in.co.dto; // ✅ Use 'entity' package, not 'dto'

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_app_users_email", columnList = "email"))
@EntityListeners(AuditingEntityListener.class) // ✅ Enables @CreatedDate/@LastModifiedDate
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Email // ✅ Validates email format
	@NotBlank
	@Column(unique = true, nullable = false, length = 255)
	private String email;

	@Column(length = 100)
	private String name;

	@Enumerated(EnumType.STRING) // ✅ Enum instead of raw String — typo-safe
	@Column(nullable = false, length = 20)
	private Role role;

	@CreatedDate // ✅ Auto-populated on insert
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate // ✅ Auto-populated on update
	@Column(nullable = false)
	private Instant updatedAt;

	// ✅ Enum defined inside the entity — keeps it co-located and prevents magic
	// strings
	public enum Role {
		ROLE_USER, ROLE_ADMIN
	}

	protected AppUser() {
	} // ✅ protected, not public — JPA only needs it internally

	public AppUser(String email, String name, Role role) {
		this.email = email;
		this.name = name;
		this.role = role;
	}

	// ✅ equals/hashCode based on business key (email), NOT database id
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AppUser other))
			return false;
		return Objects.equals(email, other.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public String toString() {
		return "AppUser{email='%s', role=%s}".formatted(email, role);
	}

	// Getters only — no unnecessary setters except for role management
	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public void setName(String name) {
		this.name = name;
	}
}