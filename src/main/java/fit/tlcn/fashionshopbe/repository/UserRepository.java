package fit.tlcn.fashionshopbe.repository;

import fit.tlcn.fashionshopbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAndIsActiveIsTrue(String emailOrPhone);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailAndIsVerifiedIsTrueAndIsActiveIsTrue(String email);

    Optional<User> findByEmailAndIsVerifiedIsFalse(String email);

    List<User> findAllByRole_NameAndAddress(String roleName, String address);
}
