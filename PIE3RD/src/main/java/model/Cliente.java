package model;

public class Cliente {

    private int id;
    private String nome;
    private String whatsapp;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    /** Link para abrir a conversa no WhatsApp (somente dígitos, com DDI 55). */
    public String getLinkWhatsapp() {
        String digitos = whatsapp == null ? "" : whatsapp.replaceAll("\\D", "");
        if (!digitos.startsWith("55")) {
            digitos = "55" + digitos;
        }
        return "https://wa.me/" + digitos;
    }

    @Override
    public String toString() {
        return nome;
    }
}
