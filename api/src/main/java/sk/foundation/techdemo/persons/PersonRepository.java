package sk.foundation.techdemo.persons;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vladmihalcea.spring.repository.HibernateRepository;

import sk.foundation.techdemo.persons.api.PersonApiRepository;
import sk.foundation.techdemo.persons.api.PersonDetailResponseDTO;

public interface PersonRepository
		extends HibernateRepository<Person>, JpaRepository<Person, Long>, PersonApiRepository {

	@Query("""
			select new sk.foundation.techdemo.persons.api.PersonDetailResponseDTO(
				p.id,
				p.firstName,
				p.lastName,
				p.email,
				p.address,
				p.state,
				p.phoneNumber
			)
			from Person p
			where p.id = :id
			""")
	PersonDetailResponseDTO getPersonDetail(@Param("id") Long id);
}
