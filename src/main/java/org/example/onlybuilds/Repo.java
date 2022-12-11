package org.example.onlybuilds;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class Repo {

    private static final String ACCOUNTS_TABLE = "accounts";
    private static final String ACCOUNT_COLUMN_NAMES = " (first_name, last_name, email, username, phone, password, salt, account_type, country, state, city, zipcode, rating, profile_picture, details) ";
    private static final String THREADS_TABLE = "threads";
    private static final String THREAD_COLUMN_NAMES = " (title, author, body, tag, creation_date, author_id) ";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EmailService emailService;

    @Transactional
    public boolean createAccount(UserData userData) throws NoSuchAlgorithmException {
        String password = userData.getPassword();
        String first_name = userData.getFirst_name();
        String last_name = userData.getLast_name();
        String email = userData.getEmail();
        String phone = userData.getPhone();
        String username = userData.getUsername();
        String account_type = userData.getAccount_type();
        String country = userData.getCountry();
        String state = userData.getState();
        String city = userData.getCity();
        int zipcode = userData.getZipcode();
        String details = userData.getDetails();
        String profile_pic = userData.getProfile_picture();
        float rating = userData.getRating();

        if (checkUsernameExists(username))
            return false;

        password = password.trim();
        password = PasswordHash.generate(password, null);

        String sql = "INSERT INTO " + ACCOUNTS_TABLE + ACCOUNT_COLUMN_NAMES + " VALUES " + "('" + first_name + "','" + last_name + "','" + email + "','" + username + "','" + phone + "','" + password + "',?,'" + account_type + "','" + country + "','" + state + "','" + city + "'," + zipcode + "," + rating + ",'" + profile_pic + "','" + details + "')";
        return Boolean.TRUE.equals(jdbcTemplate.execute(sql, (PreparedStatementCallback<Boolean>) ps -> {
            ps.setBytes(1, PasswordHash.Salt);
            return ps.execute();
        }));
    }

    @Transactional
    public UserData getUserData(UserData userData) {
        String sql = "SELECT first_name, last_name, email, username, phone, account_type, country, state, city, zipcode, rating, profile_picture, details FROM " + ACCOUNTS_TABLE + " WHERE username='" + userData.getUsername() + "'";

        try {
            Map<String, Object> map = jdbcTemplate.queryForList(sql).get(0);
            userData.setEmail(map.get("email").toString());
            userData.setAccount_type(map.get("account_type").toString());
            userData.setCity(map.get("city").toString());
            userData.setDetails(map.get("details").toString());
            userData.setFirst_name(map.get("first_name").toString());
            userData.setLast_name(map.get("last_name").toString());
            userData.setCountry(map.get("country").toString());
            userData.setState(map.get("state").toString());
            userData.setZipcode(Integer.parseInt(map.get("zipcode").toString()));
            userData.setPhone(map.get("phone").toString());
            userData.setRating(Float.parseFloat(map.get("rating").toString()));
            userData.setProfile_picture(map.get("profile_picture").toString());
            userData.setPassword();

            return userData;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Boolean login(UserData userData) throws NoSuchAlgorithmException {
        Boolean returnValue = checkCred(userData.getUsername(), userData.getPassword());
        userData.setPassword();
        return returnValue;
    }

    @Transactional
    public void resetPassword(String email, String phone) throws NoSuchAlgorithmException {
        if (!checkEmailAndPhoneExists(email, phone))
            return;
        String plainPass = generateRandomString(10);
        String newPass = PasswordHash.generate(plainPass, null);
        emailService.send("onlywebservices@gmail.com", email, "OnlyService password reset", String.format("Your new password is %s\n", plainPass));
        String sql = "UPDATE " + ACCOUNTS_TABLE + String.format(" SET password='%s', salt=? WHERE email='%s' AND phone='%s'", newPass, email, phone);
        jdbcTemplate.execute(sql, (PreparedStatementCallback<Boolean>) ps -> {
            ps.setBytes(1, PasswordHash.Salt);
            return ps.execute();
        });
    }

    @Transactional
    public UserData getUser(String username) {
        UserData userData = new UserData();
        userData.setUsername(username);
        return getUserData(userData);
    }

    @Transactional
    public UserData updateUser(UserData userData) {
        String first_name = userData.getFirst_name();
        String last_name = userData.getLast_name();
        String username = userData.getUsername();
        String phone = userData.getPhone();
        String account_type = userData.getAccount_type();
        String country = userData.getCountry();
        String state = userData.getState();
        String city = userData.getCity();
        int zipcode = userData.getZipcode();
        String details = userData.getDetails();
        String profile_pic = userData.getProfile_picture();
        float rating = userData.getRating();
        String sql = "UPDATE " + ACCOUNTS_TABLE + String.format(" SET first_name='%s', last_name='%s', username='%s', phone='%s', account_type='%s', country='%s', state='%s', city='%s', zipcode=%d, details='%s', profile_picture='%s', rating=%f", first_name, last_name, username, phone, account_type, country, state, city, zipcode, details, profile_pic, rating) + " WHERE email=" + "'" + userData.getEmail() + "'";
        jdbcTemplate.update(sql);
        return userData;
    }

    @Transactional
    public Boolean createThread(ThreadData threadData) {
        String title = threadData.getTitle();
        String author = threadData.getAuthor();
        String body = threadData.getBody();
        String tag = threadData.getTag();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String creation_date = dtf.format(now);
        threadData.setCreation_date(dtf.format(now));
        long author_id;
        String sql_getAuthorId = "SELECT id FROM " + ACCOUNTS_TABLE + " WHERE username='" + author + "'";
        try {
            author_id = (long) jdbcTemplate.queryForList(sql_getAuthorId).get(0).get("id");
        } catch (Exception e) {
            return false;
        }
        threadData.setAuthor_id(author_id);

        String sql = "INSERT INTO " + THREADS_TABLE + THREAD_COLUMN_NAMES + " VALUES ('" + title + "','" + author + "','" + body + "','" + tag + "','" + creation_date + "'," + author_id + ")";
        return Boolean.TRUE.equals(jdbcTemplate.execute(sql, (PreparedStatementCallback<Boolean>) PreparedStatement::execute));
    }

    @Transactional
    public ThreadData updateThread(ThreadData threadData) {
        String title = threadData.getTitle();
        String body = threadData.getBody();
        String tag = threadData.getTag();
        String sql = "UPDATE " + THREADS_TABLE + String.format(" SET title='%s', body='%s', tag='%s", title, body, tag);
        jdbcTemplate.update(sql);
        return threadData;
    }

    @Transactional
    public JSONObject getAllThreads() {
        String sql = "SELECT * FROM " + THREADS_TABLE;
        ArrayList<ThreadData> threadDataList = new ArrayList<>();
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> map : mapList) {
            ThreadData threadData = new ThreadData();
            threadData.setAuthor(map.get("author").toString());
            threadData.setTitle(map.get("title").toString());
            threadData.setBody(map.get("body").toString());
            threadData.setTag(map.get("tag").toString());
            threadData.setCreation_date(map.get("creation_date").toString());
            threadData.setAuthor_id(Integer.parseInt(map.get("author_id").toString()));
            threadDataList.add(threadData);
        }
        JSONObject obj = new JSONObject();
        obj.put("data", threadDataList);
        return obj;
    }

    private Boolean checkCred(String name, String password) throws NoSuchAlgorithmException {
        String type;
        if (name.contains("@")) {
            type = "email";
        } else {
            type = "username";
        }
        String sql = "SELECT * FROM " + ACCOUNTS_TABLE + " WHERE " + type + "='" + name + "'";
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            byte[] arr = jdbcTemplate.query(sql, new ResultSetExtractor<byte[]>() {

                @Override
                public byte[] extractData(ResultSet rs) throws SQLException, DataAccessException {
                    rs.next();
                    return rs.getBytes("salt");
                }
            });
            password = PasswordHash.generate(password, arr);
            if (password.equals(result.get(0).get("password"))) {
                // String email = (String) result.get(0).get("email");
                // String username = (String) result.get(0).get("username");
                return true;
            }
        } catch (DataAccessException e) {
            return false;
        }
        return false;
    }

    public static String generateRandomString(int length) {
        final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
        final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
        final String NUMBER = "0123456789";

        final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
        SecureRandom random = new SecureRandom();
        if (length < 1) {
            throw new IllegalArgumentException();
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }

        return sb.toString();
    }

    private Boolean checkUsernameExists(String username) {
        String sql = "SELECT * FROM " + ACCOUNTS_TABLE + " WHERE username='" + username + "'";
        try {
            if (jdbcTemplate.queryForList(sql).size() != 0)
                return true;
        } catch (DataAccessException e) {
            return false;
        }
        return false;
    }

    private Boolean checkEmailAndPhoneExists(String email, String phone) {
        String sql = "SELECT * FROM " + ACCOUNTS_TABLE + " WHERE email='" + email + "' AND phone='"+phone+"'";
        try {
            if (jdbcTemplate.queryForList(sql).size() != 0)
                return true;
        } catch (DataAccessException e) {
            return false;
        }
        return false;
    }
}
