package com.sist.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 장바구니 VO
public class CartVO {

	private int no;
	private int account, total;
	private String name, poster, price;
}
