package com.alfre.DHHotel;

import com.alfre.DHHotel.config.RabbitMQConfig;
import com.alfre.DHHotel.domain.event.BookingCreatedEvent;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import com.alfre.DHHotel.usecase.ReservationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the booking notification publishing flow.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
public class BookingServiceIntegrationTest {

    @Autowired
    private ReservationUseCase reservationUseCase;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin rabbitAdmin;

    private static final String TEST_EMAIL = "booking-client@test.com";

    private User clientUser;
    private Long testRoomId;

    @Container
    public static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6.5"))
            .withDatabaseName("dhhotel")
            .withUsername("test")
            .withPassword("test1234")
            .withInitScript("db/init.sql");

    @Container
    public static RabbitMQContainer rabbitMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-alpine"));

    /**
     * Configures dynamic properties for MariaDB and RabbitMQ Testcontainers.
     *
     * @param registry the dynamic property record where the connection properties are added
     */
    @DynamicPropertySource
    public static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("datasource.my-connection.jdbc-url", mariaDB::getJdbcUrl);
        registry.add("datasource.my-connection.username", mariaDB::getUsername);
        registry.add("datasource.my-connection.password", mariaDB::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    /**
     * Sets up a clean database and notification queue before each test.
     */
    @BeforeEach
    void setupTestData() {
        rabbitAdmin.purgeQueue(RabbitMQConfig.NOTIFICATIONS_QUEUE, false);

        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        clientUser = new User();
        clientUser.email = TEST_EMAIL;
        clientUser.password = passwordEncoder.encode("password");
        clientUser.role = Role.CLIENT;
        clientUser.id = userRepository.createUser(clientUser);

        Client client = new Client();
        client.user_id = clientUser.id;
        client.first_name = "Booking";
        client.last_name = "Client";
        client.phone = "555-0101";
        clientRepository.createClient(client);

        Room room = new Room();
        room.room_number = 301;
        room.type = RoomType.DOUBLE;
        room.price_per_night = BigDecimal.valueOf(120.00);
        room.status = RoomStatus.AVAILABLE;
        testRoomId = roomRepository.createRoom(room);
    }

    /**
     * Tests that creating a valid reservation publishes one notification message.
     */
    @Test
    void testCreateValidReservation_PublishesOneNotificationMessage() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.start_date = LocalDate.now().plusDays(3);
        newReservation.end_date = LocalDate.now().plusDays(5);

        // Act
        reservationUseCase.createReservation(newReservation, clientUser);

        // Assert
        Properties queueProperties = rabbitAdmin.getQueueProperties(RabbitMQConfig.NOTIFICATIONS_QUEUE);
        assertThat(queueProperties)
                .isNotNull()
                .containsEntry(RabbitAdmin.QUEUE_MESSAGE_COUNT, 1);

        Object message = rabbitTemplate.receiveAndConvert(RabbitMQConfig.NOTIFICATIONS_QUEUE, 5_000);
        assertThat(message).isInstanceOf(BookingCreatedEvent.class);

        BookingCreatedEvent event = (BookingCreatedEvent) message;
        assertThat(event.guestEmail()).isEqualTo(TEST_EMAIL);
        assertThat(rabbitTemplate.receiveAndConvert(RabbitMQConfig.NOTIFICATIONS_QUEUE, 200)).isNull();
    }
}
