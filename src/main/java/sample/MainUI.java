package sample;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;


@SpringUI(path=MainUI.URI)
@Theme(ValoTheme.THEME_NAME)
public class MainUI extends UI {

	public static final String URI = "";

	private static final String APPLICATION_TITLE = "Sample Application";

	@Autowired
	AuthenticationManager authenticationManager;

	private VerticalLayout rootLayout;

	private LoginLayout loginLayout;

	private VerticalLayout mainLayout;

	@Override
	protected void init(VaadinRequest request) {
		getPage().setTitle(APPLICATION_TITLE);
		if (rootLayout == null) {
			rootLayout = new VerticalLayout();
			rootLayout.setSizeFull();
			rootLayout.addComponent(getLoginLayout());
			rootLayout.setComponentAlignment(getLoginLayout(), Alignment.MIDDLE_CENTER);
			rootLayout.addComponent(getMainLayout());
			setContent(rootLayout);
		}

		if (isAuthenticated()) {
			showMainLayout();
		} else {
			showLoginLayout();
		}
	}

	private LoginLayout getLoginLayout() {
		if (loginLayout == null) {
			loginLayout = new LoginLayout();
		}
		return loginLayout;
	}

	private VerticalLayout getMainLayout() {
		if (mainLayout == null) {
			mainLayout = new VerticalLayout();
			mainLayout.addComponent(new Label("Main画面"));
			mainLayout.addComponent(new Button("ユーザー名を確認", e -> {
				String username = null;
				SecurityContext securityContext = SecurityContextHolder.getContext();
				Authentication authentication = securityContext.getAuthentication();
				if (authentication != null) {
					Object principal = authentication.getPrincipal();
					if (principal instanceof UserDetails) {
						UserDetails userDetails = UserDetails.class.cast(principal);
						username = userDetails.getUsername();
					}
				}
				Notification.show(username);
			}));
			mainLayout.addComponent(new Button("ログアウト", new LogoutButtonListener()));
		}
		return mainLayout;
	}

	private boolean isAuthenticated() {
		boolean authenticated = false;
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication != null) {
			if (authentication.isAuthenticated()) {
				if (!(authentication instanceof AnonymousAuthenticationToken)) {
					authenticated = true;
				}
			}
		}
		return authenticated;
	}

	private void showLoginLayout() {
		getLoginLayout().setVisible(true);
		getMainLayout().setVisible(false);
	}

	private void showMainLayout() {
		getLoginLayout().setVisible(false);
		getMainLayout().setVisible(true);
	}

	private class LogoutButtonListener implements ClickListener {

		@Override
		public void buttonClick(ClickEvent event) {
			Page.getCurrent().reload();
			SecurityContextHolder.getContext().setAuthentication(null);
			VaadinSession.getCurrent().close();
		}
	}

	private class LoginLayout extends LoginForm {

		private Label failureLabel;

		private TextField usernameField;

		private PasswordField passwordField;

		private Button loginButton;

		@Override
		protected TextField createUsernameField() {
			if (usernameField == null) {
				usernameField = new TextField("ユーザー名");
				usernameField.focus();
			}
			return usernameField;
		}

		@Override
		protected PasswordField createPasswordField() {
			if (passwordField == null) {
				passwordField = new PasswordField("パスワード");
			}
			return passwordField;
		}

		@Override
		protected Button createLoginButton() {
			if (loginButton == null) {
				loginButton = new Button("ログイン");
				loginButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
				loginButton.setDisableOnClick(true);
				loginButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
				loginButton.addClickListener(new LoginButtonListener());
			}
			return loginButton;
		}

		@Override
		protected Component createContent(TextField usernameField, PasswordField passwordField, Button loginButton) {

			VerticalLayout rootLayout = new VerticalLayout();
			rootLayout.setSizeFull();

			Label titleLabel = new Label(APPLICATION_TITLE);
			titleLabel.addStyleName(ValoTheme.LABEL_H1);
			rootLayout.addComponent(titleLabel);
			rootLayout.setComponentAlignment(titleLabel, Alignment.TOP_CENTER);

			VerticalLayout centerLayout = new VerticalLayout();
			centerLayout.setSizeUndefined();
			rootLayout.addComponent(centerLayout);
			rootLayout.setComponentAlignment(centerLayout, Alignment.MIDDLE_CENTER);

			centerLayout.addComponent(getFailureLabel());
			centerLayout.setComponentAlignment(getFailureLabel(), Alignment.BOTTOM_CENTER);

			FormLayout loginForm = new FormLayout();
			loginForm.setSizeUndefined();
			loginForm.addComponent(usernameField);
			loginForm.addComponent(passwordField);
			loginForm.addComponent(loginButton);
			centerLayout.addComponent(loginForm);
			centerLayout.setComponentAlignment(loginForm, Alignment.TOP_CENTER);

			return rootLayout;
		}

		private Label getFailureLabel() {
			if (failureLabel == null) {
				failureLabel = new Label("ログインに失敗しました。");
				failureLabel.setSizeUndefined();
				failureLabel.addStyleName(ValoTheme.LABEL_FAILURE);
				failureLabel.setVisible(false);
			}
			return failureLabel;
		}

		private class LoginButtonListener implements ClickListener {

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					String username = usernameField.getValue();
					String password = passwordField.getValue();
					UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
					Authentication authentication = authenticationManager.authenticate(token);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					showMainLayout();

				} catch (AuthenticationException ex) {
					usernameField.focus();
					usernameField.selectAll();
					getFailureLabel().setVisible(true);
				} catch (Exception ex) {
					Notification.show("予期しないエラーが発生しました。 ", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
					LoggerFactory.getLogger(getClass()).error("ログイン中に予期しないエラーが発生しました。", ex);
				} finally {
					loginButton.setEnabled(true);
				}
			}
		}
	}
}
