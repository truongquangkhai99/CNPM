package com.spring.controller;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.request.WebRequest;

import com.spring.domain.Account;
import com.spring.service.AES;
import com.spring.service.AccountS;
import com.spring.service.Mail;

import it.ozimov.springboot.mail.service.exception.CannotSendEmailException;

@RequestMapping("/account")
@SessionAttributes("account")
@Controller
public class AccountC {
	@Autowired
	private AccountS accountService;
	@Autowired
	private Mail mail;
	@Autowired
	private AES aes;

	// Đăng nhập
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public String loginPage(Model model, WebRequest wr, HttpSession session) {
		String email = wr.getParameter("email");
		String password = wr.getParameter("password");
		String message = accountService.checkLogin(email, password);
		model.addAttribute("email", email);
		if (NumberUtils.isNumber(message)) {
			Integer accountID = Integer.parseInt(message);
			Account account = accountService.getAccountByID(accountID);
			model.addAttribute("account", account);
			session.setAttribute("account", account);
			return "redirect:/home";
		} else {
			model.addAttribute("email", email);
			model.addAttribute("err", message);
			return "login_page";
		}

	}

	// Quên mật khẩu
	@RequestMapping(value = "/forgot_pass", method = RequestMethod.GET)
	public String forgotPassPage(Model model, WebRequest wr, HttpServletRequest h)
			throws AddressException, UnsupportedEncodingException, CannotSendEmailException {
		String email = wr.getParameter("email");
		if (email == null || email.isEmpty()) {
			return "forgot_password";
		}
		if (!accountService.checkEmail(email)) {
			model.addAttribute("err", "email không tồn tại trong hệ thống");
			return "forgot_password";
		} else {
			String accountID = String.valueOf(accountService.getAccountByEmail(email).getIdAcc());
			String decryptString = aes.encrypt(accountID);
			Collection<InternetAddress> to = new ArrayList<>();
			to.add(new InternetAddress(email));
			String a = h.getScheme() + "://" + h.getServerName() + ":" + h.getServerPort() + "/account/change_pass?id="
					+ decryptString;
			Map<String, Object> map = new HashMap<>();
			map.put("mail", email);
			map.put("link", a);
			mail.sendMail(to, "Đổi mật khẩu", "change_pass_mail.html", map);
			model.addAttribute("suscess", "Mail đổi mật khẩu đã gửi đến tài khoản của bạn");
			return "forgot_password";
		}
	}

	// hiển thị trang đổi mật khẩu
	@RequestMapping(value = "/change_pass", method = RequestMethod.GET)
	public String changePasswordPage(HttpSession session, WebRequest wr) {
		String idAccount = wr.getParameter("id");
		if (idAccount != null) {
			session.setAttribute("idAccount", idAccount);
		} else {
			return "redirect:/404";
		}
		return "change_password";
	}

	// action cho đổi mật khẩu
	@RequestMapping(value = "/doChangePassword", method = RequestMethod.POST)
	public String dochangePass(Model m, WebRequest wr, HttpSession session) {
		String password = wr.getParameter("pass");
		String idAccount = (String) session.getAttribute("idAccount");
		try {
			// giải mã Mã người dùng
			int idAccountDeCrypt = Integer.valueOf(aes.decrypt(idAccount));
			System.err.println(idAccountDeCrypt + "lang");
			accountService.changePassword(idAccountDeCrypt, password);
			m.addAttribute("success", "Đổi mật khẩu thành công");
			return "change_password";
		} catch (Exception e) {
			return "redirect:/404";

		}
	}

	// Đăng kí tài khoản
	@RequestMapping(value = "/sign_up")
	public String signUp(Model model, WebRequest wr, HttpServletRequest h)
			throws AddressException, UnsupportedEncodingException, CannotSendEmailException {
		String email = wr.getParameter("email");
		String password = wr.getParameter("password");
		String rePassword = wr.getParameter("repassword");
		if (email == null && password == null && rePassword == null) {
			return "sign_up";
		} else if (accountService.checkEmail(email)) {
			model.addAttribute("err", "email đã tồn tại trong hệ thống");
			model.addAttribute("email", email);
			return "sign_up";
		} else if (!rePassword.equals(password)) {
			model.addAttribute("err", "mật khẩu nhập lại không đúng");
			model.addAttribute("email", email);
			return "sign_up";
		} else {
			int accountID = accountService.SignUp(email, password);
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
			String date = df.format(new Date());
			// Mã hóa ngày đăng kí và mã người dùng
			String decryptString = aes.encrypt(date + "|" + accountID);
			// Địa chỉ email người nhận
			Collection<InternetAddress> to = new ArrayList<>();

			to.add(new InternetAddress(email));
			String a = h.getScheme() + "://" + h.getServerName() + ":" + h.getServerPort() + "/account/register?id="
					+ decryptString;
			Map<String, Object> map = new HashMap<>();
			map.put("link", a);
			mail.sendMail(to, "Kích hoạt tài khoản", "register_mail.html", map);
			model.addAttribute("suscess", "chúc mừng bạn đã đăng kí thành công  hãy vào mail để kích hoạt");
			return "sign_up";
		}
	}

	// xác thực đăng ký
	@RequestMapping(value = "/register", method = RequestMethod.GET)
	public String registerAccount(Model m, WebRequest wr) {
		String encryptString = wr.getParameter("id");
		System.out.println(encryptString);
		String decryptString = null;
		try {
			decryptString = aes.decrypt(encryptString);
			StringTokenizer st = new StringTokenizer(decryptString, "|");
			System.out.println(st.countTokens());
			if (st.countTokens() != 2) {
				System.out.println("loi");
				return "redirect:/404";
			} else {
				String dateRegister = st.nextToken();
				String idAccount = st.nextToken();
				accountService.enableAccount(Integer.parseInt(idAccount), true);
				m.addAttribute("sucess", "kích hoạt thành công");
				m.addAttribute("dateRegister", dateRegister);
				return "register_page";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/404";
		}

	}

	// Kiểm tra email có tồn tại trong hệ thống hay không
	@ResponseBody
	@RequestMapping(value = "/check_email", method = RequestMethod.GET)
	public String checkEmailSignUp(WebRequest wr) {
		String email = wr.getParameter("email");
		boolean checkEmail = accountService.checkEmail(email);
		return (checkEmail) ? "email đã tồn tại trong hệ thống" : "";
	}

	// Đăng xuất
	@RequestMapping(value = "/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.invalidate();
		return "login_page";
	}

	// trang thông tin người dùng
	@RequestMapping(value = "/info")
	public String accountInfo(Model m, HttpSession session) {
		Account account = (Account) session.getAttribute("account");
		if (account == null) {
			return "redirect:/";
		}
		m.addAttribute("acc_setting", accountService.getAccountByID(account.getIdAcc()));
		m.addAttribute("title", "Thông tin tài khoản");
		return "account_setting";
	}

	// cập nhật thông tin người dùng
	@GetMapping("/update/info")
	@ResponseBody
	public String updateInfo(WebRequest w, HttpSession session) throws ParseException {
		String name = w.getParameter("name");
		boolean gender = (w.getParameter("gender").equals("1")) ? true : false;
		String job = w.getParameter("job");
		String address = w.getParameter("address");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date2 = formatter.parse(w.getParameter("birth"));
		Account account = (Account) session.getAttribute("account");
		int idAccount = account.getIdAcc();
		if (accountService.updateAccountInfo(name, date2, job, gender, address, account.getIdAcc())) {
			session.setAttribute("account", accountService.getAccountByID(idAccount));
			return "Cập nhật thành công";
		} else {
			return "Cập nhật thất bại";
		}
	}
	// lấy mật khẩu người dùng

	@PostMapping(value = "/getUserPassword")
	@ResponseBody
	public String getUserPassword(HttpSession session) {
		Account account = (Account) session.getAttribute("account");
		return account.getPassword();

	}
}