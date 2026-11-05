package ptit.home_services_marketplace.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.catalog.GeoArea;
import ptit.home_services_marketplace.catalog.GeoAreaRepository;
import ptit.home_services_marketplace.catalog.Service;
import ptit.home_services_marketplace.catalog.ServiceCategory;
import ptit.home_services_marketplace.catalog.ServiceCategoryRepository;
import ptit.home_services_marketplace.catalog.ServiceRepository;
import ptit.home_services_marketplace.identity.UserAccount;
import ptit.home_services_marketplace.identity.UserAccountRepository;
import ptit.home_services_marketplace.identity.UserAccountStatus;
import ptit.home_services_marketplace.identity.UserRole;

import java.util.UUID;

/**
 * Idempotent development seed data. Runs after Flyway (which executes during context
 * refresh, before any {@link CommandLineRunner}). Each section checks for existing rows
 * first so it can be started repeatedly without duplicating data.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@hsm.local";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private final UserAccountRepository userAccountRepository;
    private final GeoAreaRepository geoAreaRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceRepository serviceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserAccountRepository userAccountRepository,
                      GeoAreaRepository geoAreaRepository,
                      ServiceCategoryRepository serviceCategoryRepository,
                      ServiceRepository serviceRepository,
                      PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.geoAreaRepository = geoAreaRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.serviceRepository = serviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedGeoAreas();
        seedCatalog();
    }

    private void seedAdmin() {
        String emailNorm = ADMIN_EMAIL.toLowerCase();
        if (userAccountRepository.existsByEmailNorm(emailNorm)) {
            return;
        }
        UserAccount admin = new UserAccount();
        admin.setEmail(ADMIN_EMAIL);
        admin.setEmailNorm(emailNorm);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(admin);
    }

    private void seedGeoAreas() {
        if (geoAreaRepository.count() > 0) {
            return;
        }
        geoAreaRepository.save(geoArea("HN", null, "Ha Noi", "province"));
        geoAreaRepository.save(geoArea("HCM", null, "Ho Chi Minh", "province"));
        // Persist provinces before districts so the self-referencing FK is satisfied.
        geoAreaRepository.flush();
        geoAreaRepository.save(geoArea("HN-CG", "HN", "Cau Giay", "district"));
        geoAreaRepository.save(geoArea("HN-BD", "HN", "Ba Dinh", "district"));
        geoAreaRepository.save(geoArea("HCM-Q1", "HCM", "Quan 1", "district"));
        geoAreaRepository.save(geoArea("HCM-Q3", "HCM", "Quan 3", "district"));
    }

    private void seedCatalog() {
        if (serviceCategoryRepository.count() > 0) {
            return;
        }
        ServiceCategory dienLanh = serviceCategory("Dien lanh", "dien-lanh",
                "Electrical appliances and refrigeration services");
        ServiceCategory veSinh = serviceCategory("Ve sinh", "ve-sinh",
                "Home and office cleaning services");
        serviceCategoryRepository.save(dienLanh);
        serviceCategoryRepository.save(veSinh);

        serviceRepository.save(service(dienLanh.getId(), "Sua chua dieu hoa", "sua-chua-dieu-hoa",
                "Air conditioner repair"));
        serviceRepository.save(service(dienLanh.getId(), "Bao tri tu lanh", "bao-tri-tu-lanh",
                "Refrigerator maintenance"));
        serviceRepository.save(service(veSinh.getId(), "Don dep nha", "don-dep-nha",
                "Home cleaning"));
        serviceRepository.save(service(veSinh.getId(), "Ve sinh van phong", "ve-sinh-van-phong",
                "Office cleaning"));
    }

    private GeoArea geoArea(String code, String parentCode, String name, String type) {
        GeoArea area = new GeoArea();
        area.setCode(code);
        area.setParentCode(parentCode);
        area.setName(name);
        area.setType(type);
        area.setActive(true);
        return area;
    }

    private ServiceCategory serviceCategory(String name, String slug, String description) {
        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setActive(true);
        return category;
    }

    private Service service(UUID categoryId, String name, String slug, String description) {
        Service service = new Service();
        service.setCategoryId(categoryId);
        service.setName(name);
        service.setSlug(slug);
        service.setDescription(description);
        service.setActive(true);
        return service;
    }
}
