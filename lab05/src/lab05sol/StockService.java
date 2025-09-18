package lab05sol;

import observable.Observable;
import observable.Observer;

public class StockService{
	 private StockPrice stockPrice;
	 
	 public StockService() {
		 stockPrice= new StockPrice();
	 }

    public void addPrice(String stock, double price) {
        stockPrice.addPrice(stock, price);
    }
    
    public double getStockPrice(String stock) {
        return stockPrice.getPrice(stock);
    }
    public void registerTrader(Observer trader) {
        stockPrice.register(trader);
    }

    public void unregisterTrader(Observer trader) {
        stockPrice.unregister(trader);
    }
	 
	 


}
