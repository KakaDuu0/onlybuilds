package org.example.onlybuilds;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RestController
@CrossOrigin
public class Controller {

    @Autowired
    private Repo repo;

    @CrossOrigin
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String index() {
        return "/ -> this page\n\n" +
                "/login + json in body 'username' and 'password' -> log in, returns json with all data or null\n\n" +
                "/resetpwd/email=...&phone=... -> resets the password and sends it to the users email\n\n" +
                "/accounts/create + json in body with all required info (*) -> register, returns json with all data or null\n\n" +
                "/accounts/update + json in body with new data -> updates account and returns json with data\n\n" +
                "/accounts/get?username=... -> returns json with all data or null\n\n" +
                "/threads/create + json in body with all required info (**) -> creates thread, returns json with all data or null\n\n" +
                "/threads/getall -> returns a list of json objects containing all threads\n\n" +
                "/threads/update + json in body with new info (title/body/tag) -> updates thread and return json with new info\n\n\n\n" +
                "(*) userdata required info: first_name, last_name, username, email, password, phone, account_type, country, state, city, zipcode (id is assigned automatically)\n" +
                "(*) userdata optional info: details, profile_pic, rating\n" +
                "(**) threaddata required info: title, author, tag, body (creation_date and author_id are assigned automatically)";
    }


    @CrossOrigin
    @RequestMapping(value = "/accounts/create", method = RequestMethod.POST)
    public UserData createAccount(@RequestBody UserData userData) throws NoSuchAlgorithmException {
        if (repo.createAccount(userData))
            return userData;
        return null;
    }

    @CrossOrigin
    @RequestMapping(path = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserData login(@RequestBody UserData userData) throws NoSuchAlgorithmException, JsonProcessingException {
        if(repo.login(userData))
            return repo.getUserData(userData);
        return null;
    }

    @CrossOrigin
    @RequestMapping(path = "/accounts/get", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserData getUser(@RequestParam() String username) {
        return repo.getUser(username);
    }

    @CrossOrigin
    @RequestMapping(path = "/accounts/update", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserData update(@RequestBody UserData userdata){
        return repo.updateUser(userdata);
    }

    @CrossOrigin
    @RequestMapping(value = "/resetpwd", method = RequestMethod.POST)
    public void changePass(@RequestParam("email") String email, @RequestParam("phone") String phone) throws NoSuchAlgorithmException {
        repo.resetPassword(email, phone);
    }

    @CrossOrigin
    @RequestMapping(value = "/threads/create", method = RequestMethod.POST)
    public ThreadData createThread(@RequestBody ThreadData threadData) {
        if(repo.createThread(threadData))
            return threadData;
        return null;
    }

    @CrossOrigin
    @RequestMapping(value = "/threads/update", method = RequestMethod.PATCH)
    public ThreadData updateThread(@RequestBody ThreadData threadData) {
        return repo.updateThread(threadData);
    }

    @CrossOrigin
    @RequestMapping(value = "/threads/getall", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllThreads() {
        return repo.getAllThreads().toString();
    }
}
