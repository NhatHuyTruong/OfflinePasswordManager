package vn.edu.hcmus.passman.core.models;

import java.util.ArrayList;
import java.util.List;

public class Vault {
    private List<Account> accounts;

    public Vault() {
        this.accounts = new ArrayList<>();
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public void removeAccount(String accountId) {
        accounts.removeIf(acc -> acc.getId().equals(accountId));
    }
}