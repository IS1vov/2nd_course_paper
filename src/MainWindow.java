package com.bookstore;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class MainWindow {
    private final BookStore store;
    private User currentUser;
    private final Cart cart;
    private final Stage primaryStage;
    private final TabPane tabPane;
    private static final DecimalFormat RATING_FORMAT = new DecimalFormat("0.0");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public MainWindow(BookStore store, Stage primaryStage) {
        this.store = store;
        this.cart = new Cart();
        this.primaryStage = primaryStage;
        this.tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        primaryStage.setTitle("Book Store");
        primaryStage.setOnCloseRequest(e -> {
            try {
                store.getDb().close();
            } catch (SQLException ex) {
                System.err.println("Error closing database: " + ex.getMessage());
            }
            Platform.exit();
        });

        updateTabs();
        Scene scene = new Scene(tabPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        if (currentUser == null) {
            showLoginOrRegisterDialog();
        }
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 14px;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;"));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean validateFirstName(String firstName) {
        return firstName != null && firstName.length() >= 2 && firstName.length() <= 50 && firstName.matches("^[A-Za-z]+$");
    }

    private boolean validateLastName(String lastName) {
        return lastName != null && lastName.length() >= 2 && lastName.length() <= 50 && lastName.matches("^[A-Za-z]+$");
    }

    private boolean validateLogin(String login) {
        return login != null && login.length() >= 3 && login.length() <= 20 && login.matches("^[A-Za-z0-9_]+$");
    }

    private boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validateBirthDate(String birthDate) {
        if (birthDate == null || !DATE_PATTERN.matcher(birthDate).matches()) return false;
        try {
            LocalDate date = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validatePassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 50;
    }

    private boolean validatePrice(String price) {
        try {
            double value = Double.parseDouble(price);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateStock(String stock) {
        try {
            int value = Integer.parseInt(stock);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showLoginOrRegisterDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Login or Register");

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        Label label = new Label("Please login or register to continue");
        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        styleButton(loginButton);
        styleButton(registerButton);

        loginButton.setOnAction(e -> {
            dialog.close();
            showLoginDialog();
        });
        registerButton.setOnAction(e -> {
            dialog.close();
            showRegisterDialog();
        });

        vbox.getChildren().addAll(label, loginButton, registerButton);
        Scene scene = new Scene(vbox, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showLoginDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Login");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label loginLabel = new Label("Login:");
        TextField loginField = new TextField();
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Login");
        styleButton(loginButton);

        GridPane.setConstraints(loginLabel, 0, 0);
        GridPane.setConstraints(loginField, 1, 0);
        GridPane.setConstraints(passwordLabel, 0, 1);
        GridPane.setConstraints(passwordField, 1, 1);
        GridPane.setConstraints(loginButton, 1, 2);

        loginButton.setOnAction(e -> {
            String login = loginField.getText().trim();
            String password = passwordField.getText();
            if (!validateLogin(login) || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Invalid input", "Login must be 3-20 characters, password cannot be empty.");
                return;
            }
            currentUser = store.findUser(login);
            if (currentUser == null || !currentUser.authenticate(password)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid credentials");
                showLoginOrRegisterDialog();
            } else {
                dialog.close();
                updateTabs();
            }
        });

        grid.getChildren().addAll(loginLabel, loginField, passwordLabel, passwordField, loginButton);
        Scene scene = new Scene(grid, 400, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showRegisterDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Register");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label firstNameLabel = new Label("First Name:");
        TextField firstNameField = new TextField();
        Label lastNameLabel = new Label("Last Name:");
        TextField lastNameField = new TextField();
        Label loginLabel = new Label("Login:");
        TextField loginField = new TextField();
        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        Label birthDateLabel = new Label("Birth Date (YYYY-MM-DD):");
        TextField birthDateField = new TextField();
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Label confirmPasswordLabel = new Label("Confirm Password:");
        PasswordField confirmPasswordField = new PasswordField();
        Label avatarLabel = new Label("Avatar: No file selected");
        Button uploadButton = new Button("Upload Avatar");
        Button registerButton = new Button("Register");
        styleButton(uploadButton);
        styleButton(registerButton);

        GridPane.setConstraints(firstNameLabel, 0, 0);
        GridPane.setConstraints(firstNameField, 1, 0);
        GridPane.setConstraints(lastNameLabel, 0, 1);
        GridPane.setConstraints(lastNameField, 1, 1);
        GridPane.setConstraints(loginLabel, 0, 2);
        GridPane.setConstraints(loginField, 1, 2);
        GridPane.setConstraints(emailLabel, 0, 3);
        GridPane.setConstraints(emailField, 1, 3);
        GridPane.setConstraints(birthDateLabel, 0, 4);
        GridPane.setConstraints(birthDateField, 1, 4);
        GridPane.setConstraints(passwordLabel, 0, 5);
        GridPane.setConstraints(passwordField, 1, 5);
        GridPane.setConstraints(confirmPasswordLabel, 0, 6);
        GridPane.setConstraints(confirmPasswordField, 1, 6);
        GridPane.setConstraints(uploadButton, 0, 7);
        GridPane.setConstraints(avatarLabel, 1, 7);
        GridPane.setConstraints(registerButton, 1, 8);

        File[] selectedFile = {null};
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                selectedFile[0] = file;
                avatarLabel.setText(file.getName());
            }
        });

        registerButton.setOnAction(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String login = loginField.getText().trim();
            String email = emailField.getText().trim();
            String birthDate = birthDateField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (!validateFirstName(firstName) || !validateLastName(lastName) || !validateLogin(login) ||
                    !validateEmail(email) || !validateBirthDate(birthDate) || !validatePassword(password)) {
                showAlert(Alert.AlertType.ERROR, "Invalid input", "Please check all fields.");
                return;
            }
            if (login.equals("admin")) {
                showAlert(Alert.AlertType.ERROR, "Error", "Login 'admin' is reserved");
                return;
            }
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match");
                return;
            }
            if (store.findUser(login) != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Login already exists");
                return;
            }

            try {
                String avatarPath = selectedFile[0] != null ? uploadFile(selectedFile[0], "avatars") : null;
                store.registerUser(login, firstName, lastName, email, birthDate, password, avatarPath);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! Please login.");
                dialog.close();
                showLoginDialog();
            } catch (SQLException | IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + ex.getMessage());
            }
        });

        grid.getChildren().addAll(
                firstNameLabel, firstNameField, lastNameLabel, lastNameField,
                loginLabel, loginField, emailLabel, emailField,
                birthDateLabel, birthDateField, passwordLabel, passwordField,
                confirmPasswordLabel, confirmPasswordField, uploadButton, avatarLabel, registerButton
        );
        Scene scene = new Scene(grid, 500, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String uploadFile(File sourceFile, String destinationFolder) throws IOException {
        if (sourceFile == null) return null;
        File destDir = new File(destinationFolder);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destinationFolder + "/" + sourceFile.getName());
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destFile.getPath();
    }

    private void updateTabs() {
        tabPane.getTabs().clear();
        List<Category> categories = store.readCategories();

        for (Category category : categories) {
            Tab tab = new Tab(category.getName());
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            content.setBackground(new Background(new BackgroundFill(Color.BEIGE, null, null)));

            ComboBox<String> filterCombo = new ComboBox<>(FXCollections.observableArrayList(
                    "Default", "Price (Ascending)", "Price (Descending)", "Popularity (Descending)",
                    "Rating (Descending)", "Reviews (Descending)"
            ));
            filterCombo.setValue("Default");

            TableView<Book> table = new TableView<>();
            table.setRowFactory(tv -> {
                TableRow<Book> row = new TableRow<>();
                row.itemProperty().addListener((obs, oldBook, newBook) -> {
                    if (newBook != null && newBook.getStock() == 0) {
                        row.setStyle("-fx-background-color: #d3d3d3;");
                    } else {
                        row.setStyle("");
                    }
                });
                return row;
            });

            TableColumn<Book, String> nameColumn = new TableColumn<>("Name");
            nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

            TableColumn<Book, String> priceColumn = new TableColumn<>("Price");
            priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getPrice()));

            TableColumn<Book, String> ratingColumn = new TableColumn<>("Rating");
            ratingColumn.setCellValueFactory(cellData -> {
                try {
                    double avgRating = store.getDb().getBookAverageRating(cellData.getValue().getId());
                    int voteCount = store.getDb().getBookRatingCount(cellData.getValue().getId());
                    return new SimpleStringProperty(RATING_FORMAT.format(avgRating) + " (" + voteCount + " votes)");
                } catch (SQLException e) {
                    return new SimpleStringProperty("N/A");
                }
            });

            TableColumn<Book, String> stockColumn = new TableColumn<>("Stock");
            stockColumn.setCellValueFactory(cellData -> {
                int stock = cellData.getValue().getStock();
                return new SimpleStringProperty(stock > 0 ? String.valueOf(stock) : "Sold Out");
            });

            table.getColumns().addAll(nameColumn, priceColumn, ratingColumn, stockColumn);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            HBox buttons = new HBox(10);
            Button addToCartButton = new Button("Add to Cart");
            Button rateButton = new Button("Rate Book");
            Button reviewButton = new Button("View/Add Review");
            styleButton(addToCartButton);
            styleButton(rateButton);
            styleButton(reviewButton);
            buttons.getChildren().addAll(addToCartButton, rateButton, reviewButton);

            if (currentUser != null && currentUser.getRole().equals("Admin")) {
                Button addBookButton = new Button("Add Book");
                Button editBookButton = new Button("Edit Book");
                Button deleteBookButton = new Button("Delete Book");
                styleButton(addBookButton);
                styleButton(editBookButton);
                styleButton(deleteBookButton);
                buttons.getChildren().addAll(addBookButton, editBookButton, deleteBookButton);

                addBookButton.setOnAction(e -> showAddBookDialog(category));
                editBookButton.setOnAction(e -> {
                    Book selected = table.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        showEditBookDialog(selected);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to edit.");
                    }
                });
                deleteBookButton.setOnAction(e -> {
                    Book selected = table.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        try {
                            store.getDb().deleteBook(selected.getId());
                            updateTabs();
                        } catch (SQLException ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete book: " + ex.getMessage());
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to delete.");
                    }
                });
            }

            addToCartButton.setOnAction(e -> {
                Book selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (cart.addBook(selected)) {
                        try {
                            store.getDb().decreaseStock(selected.getId());
                            store.getDb().savePurchase(currentUser.getLogin(), selected.getId());
                            updateTabs();
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Book added to cart!");
                        } catch (SQLException ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add to cart: " + ex.getMessage());
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Out of Stock", "This book is sold out.");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to add to cart.");
                }
            });

            rateButton.setOnAction(e -> {
                Book selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showRateBookDialog(selected);
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to rate.");
                }
            });

            reviewButton.setOnAction(e -> {
                Book selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showReviewsDialog(selected);
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to review.");
                }
            });

            filterCombo.setOnAction(e -> {
                try {
                    table.setItems(FXCollections.observableArrayList(
                            store.getFilteredBooks(category.getName(), filterCombo.getValue())
                    ));
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to filter books: " + ex.getMessage());
                }
            });

            try {
                table.setItems(FXCollections.observableArrayList(
                        store.getFilteredBooks(category.getName(), "Default")
                ));
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load books: " + ex.getMessage());
            }

            content.getChildren().addAll(filterCombo, table, buttons);
            tab.setContent(content);
            tabPane.getTabs().add(tab);
        }

        Tab cartTab = new Tab("Cart");
        VBox cartContent = new VBox(10);
        cartContent.setPadding(new Insets(10));
        TableView<Book> cartTable = new TableView<>();
        TableColumn<Book, String> cartNameColumn = new TableColumn<>("Name");
        cartNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        TableColumn<Book, String> cartPriceColumn = new TableColumn<>("Price");
        cartPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getPrice()));
        cartTable.getColumns().addAll(cartNameColumn, cartPriceColumn);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setItems(FXCollections.observableArrayList(cart.getBooks()));

        Button removeFromCartButton = new Button("Remove from Cart");
        Button clearCartButton = new Button("Clear Cart");
        styleButton(removeFromCartButton);
        styleButton(clearCartButton);

        removeFromCartButton.setOnAction(e -> {
            Book selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int index = cartTable.getSelectionModel().getSelectedIndex();
                cart.removeBook(index);
                cartTable.setItems(FXCollections.observableArrayList(cart.getBooks()));
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to remove.");
            }
        });

        clearCartButton.setOnAction(e -> {
            cart.clear();
            cartTable.setItems(FXCollections.observableArrayList(cart.getBooks()));
        });

        cartContent.getChildren().addAll(cartTable, removeFromCartButton, clearCartButton);
        cartTab.setContent(cartContent);
        tabPane.getTabs().add(cartTab);

        Tab messagesTab = new Tab("Messages");
        VBox messagesContent = new VBox(10);
        messagesContent.setPadding(new Insets(10));

        if (currentUser == null) {
            // Если пользователь не вошел, показываем сообщение о необходимости входа
            Label loginRequiredLabel = new Label("Please log in to view messages.");
            messagesContent.getChildren().add(loginRequiredLabel);
        } else {
            // Пользователь вошел, загружаем сообщения
            ListView<String> messagesList = new ListView<>();
            ComboBox<String> recipientCombo = new ComboBox<>();
            TextArea messageArea = new TextArea();
            messageArea.setPromptText("Type your message...");
            Button sendMessageButton = new Button("Send Message");
            styleButton(sendMessageButton);


                List<User> users = store.getAllUsers();
                recipientCombo.setItems(FXCollections.observableArrayList(
                        users.stream().map(User::getLogin).filter(login -> !login.equals(currentUser.getLogin())).toList()
                ));
                List<Message> messages = store.getMessages(currentUser.getLogin());
                messagesList.setItems(FXCollections.observableArrayList(
                        messages.stream().map(msg -> String.format(
                                "[%s] %s -> %s: %s",
                                msg.getTimestamp(), msg.getSenderLogin(), msg.getReceiverLogin(), msg.getText()
                        )).toList()
                ));


            sendMessageButton.setOnAction(e -> {
                String recipient = recipientCombo.getValue();
                String text = messageArea.getText().trim();
                if (recipient == null || text.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please select a recipient and enter a message.");
                    return;
                }
                try {
                    store.sendMessage(currentUser.getLogin(), recipient, text);
                    List<Message> newMessages = store.getMessages(currentUser.getLogin());
                    messagesList.setItems(FXCollections.observableArrayList(
                            newMessages.stream().map(msg -> String.format(
                                    "[%s] %s -> %s: %s",
                                    msg.getTimestamp(), msg.getSenderLogin(), msg.getReceiverLogin(), msg.getText()
                            )).toList()
                    ));
                    messageArea.clear();
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to send message: " + ex.getMessage());
                }
            });

            messagesContent.getChildren().addAll(recipientCombo, messageArea, sendMessageButton, messagesList);
        }

        messagesTab.setContent(messagesContent);
        tabPane.getTabs().add(messagesTab);

        Tab accountTab = new Tab("Account");
        VBox accountContent = new VBox(10);
        accountContent.setPadding(new Insets(10));
        Label userInfo = new Label(currentUser != null ?
                String.format("Login: %s\nName: %s\nEmail: %s\nBirth Date: %s\nRole: %s",
                        currentUser.getLogin(), currentUser.getName(), currentUser.getEmail(),
                        currentUser.getBirthDate(), currentUser.getRole()) :
                "Not logged in");
        Button logoutButton = new Button("Logout");
        styleButton(logoutButton);

        logoutButton.setOnAction(e -> {
            currentUser = null;
            cart.clear();
            updateTabs();
            showLoginOrRegisterDialog();
        });

        accountContent.getChildren().addAll(userInfo, logoutButton);
        if (currentUser != null && currentUser.getRole().equals("Admin")) {
            Button manageUsersButton = new Button("Manage Users");
            styleButton(manageUsersButton);
            manageUsersButton.setOnAction(e -> showManageUsersDialog());
            accountContent.getChildren().add(manageUsersButton);
        }
        accountTab.setContent(accountContent);
        tabPane.getTabs().add(accountTab);
    }

    private void showAddBookDialog(Category category) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Add Book");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Label priceLabel = new Label("Price:");
        TextField priceField = new TextField();
        Label descriptionLabel = new Label("Description:");
        TextArea descriptionArea = new TextArea();
        Label stockLabel = new Label("Stock:");
        TextField stockField = new TextField();
        Label coverLabel = new Label("Cover: No file selected");
        Button uploadButton = new Button("Upload Cover");
        Button addButton = new Button("Add Book");
        styleButton(uploadButton);
        styleButton(addButton);

        GridPane.setConstraints(nameLabel, 0, 0);
        GridPane.setConstraints(nameField, 1, 0);
        GridPane.setConstraints(priceLabel, 0, 1);
        GridPane.setConstraints(priceField, 1, 1);
        GridPane.setConstraints(descriptionLabel, 0, 2);
        GridPane.setConstraints(descriptionArea, 1, 2);
        GridPane.setConstraints(stockLabel, 0, 3);
        GridPane.setConstraints(stockField, 1, 3);
        GridPane.setConstraints(uploadButton, 0, 4);
        GridPane.setConstraints(coverLabel, 1, 4);
        GridPane.setConstraints(addButton, 1, 5);

        File[] selectedFile = {null};
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                selectedFile[0] = file;
                coverLabel.setText(file.getName());
            }
        });

        addButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String description = descriptionArea.getText().trim();
            String stockText = stockField.getText().trim();

            if (name.isEmpty() || !validatePrice(priceText) || !validateStock(stockText)) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please check name, price, and stock.");
                return;
            }

            try {
                double price = Double.parseDouble(priceText);
                int stock = Integer.parseInt(stockText);
                String coverPath = selectedFile[0] != null ? uploadFile(selectedFile[0], "covers") : null;
                Book book = new Book(0, name, price, description, category, coverPath, stock);
                store.getDb().saveBook(book);
                updateTabs();
                dialog.close();
            } catch (SQLException | IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add book: " + ex.getMessage());
            }
        });

        grid.getChildren().addAll(
                nameLabel, nameField, priceLabel, priceField,
                descriptionLabel, descriptionArea, stockLabel, stockField,
                uploadButton, coverLabel, addButton
        );
        Scene scene = new Scene(grid, 500, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showEditBookDialog(Book book) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Edit Book");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(book.getName());
        Label priceLabel = new Label("Price:");
        TextField priceField = new TextField(String.valueOf(book.getPrice()));
        Label descriptionLabel = new Label("Description:");
        TextArea descriptionArea = new TextArea(book.getDescription());
        Label stockLabel = new Label("Stock:");
        TextField stockField = new TextField(String.valueOf(book.getStock()));
        Label coverLabel = new Label("Cover: " + (book.getCoverPath() != null ? new File(book.getCoverPath()).getName() : "No file"));
        Button uploadButton = new Button("Upload New Cover");
        Button saveButton = new Button("Save Changes");
        styleButton(uploadButton);
        styleButton(saveButton);

        GridPane.setConstraints(nameLabel, 0, 0);
        GridPane.setConstraints(nameField, 1, 0);
        GridPane.setConstraints(priceLabel, 0, 1);
        GridPane.setConstraints(priceField, 1, 1);
        GridPane.setConstraints(descriptionLabel, 0, 2);
        GridPane.setConstraints(descriptionArea, 1, 2);
        GridPane.setConstraints(stockLabel, 0, 3);
        GridPane.setConstraints(stockField, 1, 3);
        GridPane.setConstraints(uploadButton, 0, 4);
        GridPane.setConstraints(coverLabel, 1, 4);
        GridPane.setConstraints(saveButton, 1, 5);

        File[] selectedFile = {null};
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                selectedFile[0] = file;
                coverLabel.setText(file.getName());
            }
        });

        saveButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String description = descriptionArea.getText().trim();
            String stockText = stockField.getText().trim();

            if (name.isEmpty() || !validatePrice(priceText) || !validateStock(stockText)) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please check name, price, and stock.");
                return;
            }

            try {
                double price = Double.parseDouble(priceText);
                int stock = Integer.parseInt(stockText);
                String coverPath = selectedFile[0] != null ? uploadFile(selectedFile[0], "covers") : book.getCoverPath();
                store.getDb().updateBook(book.getId(), name, price, description, coverPath, stock);
                updateTabs();
                dialog.close();
            } catch (SQLException | IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update book: " + ex.getMessage());
            }
        });

        grid.getChildren().addAll(
                nameLabel, nameField, priceLabel, priceField,
                descriptionLabel, descriptionArea, stockLabel, stockField,
                uploadButton, coverLabel, saveButton
        );
        Scene scene = new Scene(grid, 500, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showRateBookDialog(Book book) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Rate Book");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        Label label = new Label("Rate " + book.getName() + " (1-5):");
        ComboBox<Integer> ratingCombo = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        Button submitButton = new Button("Submit Rating");
        styleButton(submitButton);

        try {
            Integer currentRating = store.getDb().getUserBookRating(currentUser.getLogin(), book.getId());
            if (currentRating != null) {
                ratingCombo.setValue(currentRating);
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load current rating: " + ex.getMessage());
        }

        submitButton.setOnAction(e -> {
            Integer rating = ratingCombo.getValue();
            if (rating == null) {
                showAlert(Alert.AlertType.WARNING, "No Rating", "Please select a rating.");
                return;
            }
            try {
                store.getDb().saveBookRating(currentUser.getLogin(), book.getId(), rating);
                updateTabs();
                dialog.close();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save rating: " + ex.getMessage());
            }
        });

        vbox.getChildren().addAll(label, ratingCombo, submitButton);
        Scene scene = new Scene(vbox, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showReviewsDialog(Book book) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Reviews for " + book.getName());

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        TreeView<String> reviewTree = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Reviews");
        reviewTree.setRoot(root);
        reviewTree.setShowRoot(false);

        TextArea newReviewArea = new TextArea();
        newReviewArea.setPromptText("Write your review...");
        Button addReviewButton = new Button("Add Review");
        styleButton(addReviewButton);

        addReviewButton.setOnAction(e -> {
            String text = newReviewArea.getText().trim();
            if (text.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Review cannot be empty.");
                return;
            }
            try {
                TreeItem<String> selected = reviewTree.getSelectionModel().getSelectedItem();
                Integer parentId = null;
                if (selected != null && !selected.getValue().equals("Reviews")) {
                    for (Review review : store.getReviews(book.getId())) {
                        if (selected.getValue().contains("[" + review.getUserLogin() + "]")) {
                            parentId = review.getId();
                            break;
                        }
                    }
                }
                store.addReview(book.getId(), currentUser.getLogin(), text, parentId);
                newReviewArea.clear();
                updateReviewTree(reviewTree, book);
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add review: " + ex.getMessage());
            }
        });

        updateReviewTree(reviewTree, book);
        vbox.getChildren().addAll(reviewTree, newReviewArea, addReviewButton);
        Scene scene = new Scene(vbox, 600, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void updateReviewTree(TreeView<String> reviewTree, Book book) {
        TreeItem<String> root = reviewTree.getRoot();
        root.getChildren().clear();


            List<Review> reviews = store.getReviews(book.getId());
            for (Review review : reviews) {
                if (review.getParentId() == null) {
                    TreeItem<String> item = new TreeItem<>(
                            "[" + review.getUserLogin() + "] " + review.getText() +
                                    " (Likes: " + review.getLikes() + ", Dislikes: " + review.getDislikes() + ")"
                    );
                    addReplies(item, review, reviews);
                    root.getChildren().add(item);
                }
            }

    }

    private void addReplies(TreeItem<String> parentItem, Review parentReview, List<Review> reviews) {
        for (Review review : reviews) {
            if (review.getParentId() != null && review.getParentId() == parentReview.getId()) {
                TreeItem<String> item = new TreeItem<>(
                        "[" + review.getUserLogin() + "] " + review.getText() +
                                " (Likes: " + review.getLikes() + ", Dislikes: " + review.getDislikes() + ")"
                );
                addReplies(item, review, reviews);
                parentItem.getChildren().add(item);
            }
        }
    }

    private void showManageUsersDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Manage Users");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        TableView<User> userTable = new TableView<>();
        TableColumn<User, String> loginColumn = new TableColumn<>("Login");
        loginColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLogin()));
        TableColumn<User, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        TableColumn<User, String> roleColumn = new TableColumn<>("Role");
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));
        userTable.getColumns().addAll(loginColumn, nameColumn, roleColumn);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


            userTable.setItems(FXCollections.observableArrayList(store.getAllUsers()));


        Button changeRoleButton = new Button("Change Role");
        Button deleteUserButton = new Button("Delete User");
        styleButton(changeRoleButton);
        styleButton(deleteUserButton);

        changeRoleButton.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getLogin().equals("admin")) {
                    showAlert(Alert.AlertType.WARNING, "Restricted", "Cannot change role of admin.");
                    return;
                }
                String newRole = selected.getRole().equals("Admin") ? "Client" : "Admin";
                try {
                    store.getDb().updateUserRole(selected.getLogin(), newRole);
                    userTable.setItems(FXCollections.observableArrayList(store.getAllUsers()));
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to change role: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user.");
            }
        });

        deleteUserButton.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getLogin().equals("admin")) {
                    showAlert(Alert.AlertType.WARNING, "Restricted", "Cannot delete admin.");
                    return;
                }
                try {
                    store.removeUser(selected.getLogin());
                    userTable.setItems(FXCollections.observableArrayList(store.getAllUsers()));
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user.");
            }
        });

        vbox.getChildren().addAll(userTable, changeRoleButton, deleteUserButton);
        Scene scene = new Scene(vbox, 600, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}