import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * STUDENT MANAGEMENT SYSTEM
 *
 * Architecture Layers:
 * 1. DOMAIN LAYER - Student entity (business logic)
 * 2. APPLICATION LAYER - StudentService (use cases)
 * 3. INFRASTRUCTURE LAYER - InMemoryRepository (data storage)
 * 4. PRESENTATION LAYER - JavaFX UI (this main class)
 */
public class StudentApp extends Application {

    // ============================================
    // DOMAIN LAYER - Enterprise Business Rules
    // ============================================

    /**
     * Domain Entity: Student
     * Contains business logic (age calculation, full name)
     */
    static class Student {
        private String id;              // Nomor Induk Mahasiswa
        private String namaDepan;
        private String namaBelakang;    // Optional
        private LocalDate tanggalLahir;

        public Student(String id, String namaDepan, String namaBelakang, LocalDate tanggalLahir) {
            this.id = id;
            this.namaDepan = namaDepan;
            this.namaBelakang = namaBelakang;
            this.tanggalLahir = tanggalLahir;
        }

        // Business Logic: Calculate Age
        public int getUsia() {
            return Period.between(tanggalLahir, LocalDate.now()).getYears();
        }

        // Business Logic: Get Full Name
        public String getNamaLengkap() {
            if (namaBelakang == null || namaBelakang.trim().isEmpty()) {
                return namaDepan;
            }
            return namaDepan + " " + namaBelakang;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNamaDepan() { return namaDepan; }
        public void setNamaDepan(String namaDepan) { this.namaDepan = namaDepan; }
        public String getNamaBelakang() { return namaBelakang; }
        public void setNamaBelakang(String namaBelakang) { this.namaBelakang = namaBelakang; }
        public LocalDate getTanggalLahir() { return tanggalLahir; }
        public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }
    }

    /**
     * Domain Repository Interface
     * Defines operations without implementation
     */
    interface StudentRepository {
        void save(Student student);
        Optional<Student> findById(String id);
        List<Student> findAll();
        void deleteById(String id);
        void update(Student student);
    }

    // ============================================
    // INFRASTRUCTURE LAYER - Technical Details
    // ============================================

    /**
     * Infrastructure: In-Memory Repository Implementation
     * Stores data in memory (no database needed)
     */
    static class InMemoryStudentRepository implements StudentRepository {
        private List<Student> students = new ArrayList<>();

        @Override
        public void save(Student student) {
            students.add(student);
        }

        @Override
        public Optional<Student> findById(String id) {
            return students.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<Student> findAll() {
            return new ArrayList<>(students);
        }

        @Override
        public void deleteById(String id) {
            students.removeIf(s -> s.getId().equals(id));
        }

        @Override
        public void update(Student student) {
            deleteById(student.getId());
            save(student);
        }
    }

    // ============================================
    // APPLICATION LAYER - Use Cases
    // ============================================

    /**
     * Application Service: Student Use Cases
     * Contains business rules and orchestration
     */
    static class StudentService {
        private final StudentRepository repository;

        public StudentService(StudentRepository repository) {
            this.repository = repository;
        }

        // Use Case: Create Student
        public void createStudent(String id, String namaDepan, String namaBelakang, LocalDate tanggalLahir) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID tidak boleh kosong");
            }
            if (namaDepan == null || namaDepan.trim().isEmpty()) {
                throw new IllegalArgumentException("Nama Depan tidak boleh kosong");
            }
            if (repository.findById(id).isPresent()) {
                throw new IllegalArgumentException("ID sudah terdaftar");
            }

            Student student = new Student(id, namaDepan, namaBelakang, tanggalLahir);
            repository.save(student);
        }

        // Use Case: Update Student
        public void updateStudent(String id, String namaDepan, String namaBelakang, LocalDate tanggalLahir) {
            Student student = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Student tidak ditemukan"));

            student.setNamaDepan(namaDepan);
            student.setNamaBelakang(namaBelakang);
            student.setTanggalLahir(tanggalLahir);
            repository.update(student);
        }

        // Use Case: Delete Student
        public void deleteStudent(String id) {
            if (!repository.findById(id).isPresent()) {
                throw new IllegalArgumentException("Student tidak ditemukan");
            }
            repository.deleteById(id);
        }

        // Use Case: Get All Students
        public List<Student> getAllStudents() {
            return repository.findAll();
        }

        // Use Case: Find Student
        public Optional<Student> findStudent(String id) {
            return repository.findById(id);
        }
    }

    // ============================================
    // PRESENTATION LAYER - User Interface
    // ============================================

    /**
     * Display DTO for Table View
     */
    public static class StudentDisplay {
        private String nomorIndukMahasiswa;
        private String namaLengkap;
        private int usia;

        public StudentDisplay(String nomorIndukMahasiswa, String namaLengkap, int usia) {
            this.nomorIndukMahasiswa = nomorIndukMahasiswa;
            this.namaLengkap = namaLengkap;
            this.usia = usia;
        }

        public String getNomorIndukMahasiswa() { return nomorIndukMahasiswa; }
        public String getNamaLengkap() { return namaLengkap; }
        public int getUsia() { return usia; }
    }

    // UI Components
    private StudentService studentService;
    private TableView<StudentDisplay> tableView;
    private TextField idField, namaDepanField, namaBelakangField;
    private DatePicker tanggalLahirPicker;
    private Button saveButton, cancelButton;
    private Label statusLabel;
    private boolean isEditMode = false;

    @Override
    public void start(Stage primaryStage) {
        // Initialize service with repository (Dependency Injection)
        StudentRepository repository = new InMemoryStudentRepository();
        studentService = new StudentService(repository);

        // Create UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("📚 SISTEM MANAJEMEN MAHASISWA");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Status Label
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        // Form Section
        VBox formSection = createFormSection();

        // Table Section
        VBox tableSection = createTableSection();

        root.getChildren().addAll(titleLabel, subtitleLabel, statusLabel, formSection, tableSection);

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("Student Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createFormSection() {
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(15));
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label formTitle = new Label("➕ Tambah Mahasiswa Baru");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Form Fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label idLabel = new Label("NIM *:");
        idField = new TextField();
        idField.setPromptText("Nomor Induk Mahasiswa");

        Label namaDepanLabel = new Label("Nama Depan *:");
        namaDepanField = new TextField();
        namaDepanField.setPromptText("Contoh: Budi");

        Label namaBelakangLabel = new Label("Nama Belakang:");
        namaBelakangField = new TextField();
        namaBelakangField.setPromptText("Opsional");

        Label tanggalLabel = new Label("Tanggal Lahir *:");
        tanggalLahirPicker = new DatePicker();
        tanggalLahirPicker.setPromptText("Pilih tanggal");

        grid.add(idLabel, 0, 0);
        grid.add(idField, 1, 0);
        grid.add(namaDepanLabel, 2, 0);
        grid.add(namaDepanField, 3, 0);
        grid.add(namaBelakangLabel, 0, 1);
        grid.add(namaBelakangField, 1, 1);
        grid.add(tanggalLabel, 2, 1);
        grid.add(tanggalLahirPicker, 3, 1);

        // Buttons
        HBox buttonBox = new HBox(10);
        saveButton = new Button("💾 Simpan");
        saveButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setOnAction(e -> handleSave());

        cancelButton = new Button("❌ Batal");
        cancelButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        cancelButton.setVisible(false);
        cancelButton.setOnAction(e -> resetForm());

        buttonBox.getChildren().addAll(saveButton, cancelButton);

        formBox.getChildren().addAll(formTitle, grid, buttonBox);
        return formBox;
    }

    private VBox createTableSection() {
        VBox tableBox = new VBox(10);
        tableBox.setPadding(new Insets(15));
        tableBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label tableTitle = new Label("📋 Daftar Mahasiswa");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Table
        tableView = new TableView<>();

        TableColumn<StudentDisplay, String> nimCol = new TableColumn<>("Nomor Induk Mahasiswa");
        nimCol.setCellValueFactory(new PropertyValueFactory<>("nomorIndukMahasiswa"));
        nimCol.setPrefWidth(200);

        TableColumn<StudentDisplay, String> namaCol = new TableColumn<>("Nama Lengkap");
        namaCol.setCellValueFactory(new PropertyValueFactory<>("namaLengkap"));
        namaCol.setPrefWidth(250);

        TableColumn<StudentDisplay, Integer> usiaCol = new TableColumn<>("Usia");
        usiaCol.setCellValueFactory(new PropertyValueFactory<>("usia"));
        usiaCol.setPrefWidth(100);

        TableColumn<StudentDisplay, Void> actionCol = new TableColumn<>("Aksi");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button deleteBtn = new Button("🗑️ Hapus");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                editBtn.setOnAction(e -> {
                    StudentDisplay data = getTableView().getItems().get(getIndex());
                    handleEdit(data.getNomorIndukMahasiswa());
                });

                deleteBtn.setOnAction(e -> {
                    StudentDisplay data = getTableView().getItems().get(getIndex());
                    handleDelete(data.getNomorIndukMahasiswa());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableView.getColumns().addAll(nimCol, namaCol, usiaCol, actionCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableBox.getChildren().addAll(tableTitle, tableView);
        return tableBox;
    }

    private void handleSave() {
        try {
            String id = idField.getText();
            String namaDepan = namaDepanField.getText();
            String namaBelakang = namaBelakangField.getText();
            LocalDate tanggalLahir = tanggalLahirPicker.getValue();

            if (tanggalLahir == null) {
                showError("Tanggal lahir harus diisi!");
                return;
            }

            if (isEditMode) {
                studentService.updateStudent(id, namaDepan, namaBelakang, tanggalLahir);
                showSuccess("✅ Data berhasil diupdate!");
            } else {
                studentService.createStudent(id, namaDepan, namaBelakang, tanggalLahir);
                showSuccess("✅ Mahasiswa berhasil ditambahkan!");
            }

            resetForm();
            refreshTable();

        } catch (IllegalArgumentException e) {
            showError("❌ " + e.getMessage());
        } catch (Exception e) {
            showError("Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void handleEdit(String id) {
        Optional<Student> studentOpt = studentService.findStudent(id);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            isEditMode = true;

            idField.setText(student.getId());
            idField.setDisable(true);
            namaDepanField.setText(student.getNamaDepan());
            namaBelakangField.setText(student.getNamaBelakang());
            tanggalLahirPicker.setValue(student.getTanggalLahir());

            saveButton.setText("💾 Update");
            cancelButton.setVisible(true);
        }
    }

    private void handleDelete(String id) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Mahasiswa");
        alert.setContentText("Apakah Anda yakin ingin menghapus mahasiswa ini?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                studentService.deleteStudent(id);
                showSuccess("Mahasiswa berhasil dihapus!");
                refreshTable();
            } catch (Exception e) {
                showError("Gagal menghapus: " + e.getMessage());
            }
        }
    }

    private void resetForm() {
        idField.clear();
        idField.setDisable(false);
        namaDepanField.clear();
        namaBelakangField.clear();
        tanggalLahirPicker.setValue(null);
        saveButton.setText("💾 Simpan");
        cancelButton.setVisible(false);
        isEditMode = false;
    }

    private void refreshTable() {
        ObservableList<StudentDisplay> data = FXCollections.observableArrayList();
        for (Student student : studentService.getAllStudents()) {
            data.add(new StudentDisplay(
                    student.getId(),
                    student.getNamaLengkap(),
                    student.getUsia()
            ));
        }
        tableView.setItems(data);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}