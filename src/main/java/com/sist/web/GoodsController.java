package com.sist.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.sist.dao.CartVO;
import com.sist.dao.GoodsDAO;
import com.sist.dao.GoodsVO;

import java.util.*;

@Controller
public class GoodsController {

	@Autowired
	private GoodsDAO dao;
	
	//////////////////////////////
	
	// 사용자 요청 처리
	

	
	// 전체 목록 요청 => GetMapping => if
	@GetMapping("goods/list.do")
	public String goods_list(String page, Model model, HttpServletRequest request)
	{
		if(page == null)
			page = "1";
		
		int curpage = Integer.parseInt(page);
		Map map = new HashMap();
		
		int rowSize = 12; // 페이지 당 상품 개수
		int start = (rowSize*curpage)-(rowSize-1);
		int end = rowSize*curpage;
		
		map.put("start", start);
		map.put("end", end);
		
		
		// 긴 문자열 자르기
		List<GoodsVO> list = dao.goodsListData(map);
		
		for(GoodsVO vo:list)
		{
			String name = vo.getGoods_name();
			
			if(name.length() > 25)
			{
				name = name.substring(0, 25) + "...";
				vo.setGoods_name(name);
			}
			vo.setGoods_name(name);
		}
		
		int totalpage = dao.goodsTotalPage();
		
		// cookie
		Cookie[] cookies = request.getCookies();
		List<GoodsVO> cList = new ArrayList<GoodsVO>();
		
		if(cookies != null)
		{
			for(int i=cookies.length-1; i>=0; i--) // 최신 순부터 가져오기
			{
				if(cookies[i].getName().startsWith("goods"))
				{
					cookies[i].setPath("/"); // 경로 지정
					String no = cookies[i].getValue();
					
					// no에 해당되는 데이터를 읽어온다
					GoodsVO vo = dao.goodsDetailData(Integer.parseInt(no));
					cList.add(vo);
				}
			}
		}
		
		model.addAttribute("curpage", curpage);
		model.addAttribute("totalpage",totalpage);
		model.addAttribute("list", list);
		model.addAttribute("cList", cList); // cookie가 담긴 데이터
		model.addAttribute("size", cList.size()); // cookie 데이터 없으면 '없음' 문구 출력, 있으면 cookie 데이터 출력
		
		return "goods/list";
	}
	
	// cookie 저장
	@GetMapping("goods/detail_before.do")
	public String goods_detail_before(int no, HttpServletResponse response)
	{
		Cookie cookie = new Cookie("goods" + no, String.valueOf(no)); // 단점 => 문자열만 저장 가능, 클라이언트 브라우저에 저장
		cookie.setPath("/");
		cookie.setMaxAge(60*60*24); // 24시간 저장
		response.addCookie(cookie);
		
		return "redirect: detail.do?no=" + no; // cookie 담고 상세 보기로 이동
	}
	
	// 상세 보기 
	@GetMapping("goods/detail.do")
	public String goods_detail(int no, Model model, HttpServletResponse response)
	{
		GoodsVO vo = dao.goodsDetailData(no);
		vo.setPrice(Integer.parseInt(vo.getGoods_price().replaceAll("[^0-9]", "").trim()));
		// goods_price의 기호 제거해서 -> price에 담음
		// 20,000원 => 20000
		
		model.addAttribute("vo", vo);
		
		return "goods/detail";
	}
	
	// cookie 삭제
	@GetMapping("goods/cookie_delete.do")
	public String goods_cookie_delete(int no, HttpServletRequest request, HttpServletResponse response)
	{
		Cookie[] cookies = request.getCookies();
		
		for(int i=0; i<cookies.length; i++)
		{
			if(cookies[i].getName().equals("goods" + no)) // 1개씩 삭제 시 if 사용 => 전체 삭제 시 if 사용X
			{
				cookies[i].setPath("/");
				cookies[i].setMaxAge(0);
				
				response.addCookie(cookies[i]); // 삭제
				break;
			}
		}
		
		return "redirect: list.do"; // cookie 삭제 후 list.do에 이동
	}
	
	// cookie 전체 삭제
	@GetMapping("goods/cookie_all_delete.do")
	public String goods_cookie_all_delete(HttpServletRequest request, HttpServletResponse response)
	{
		Cookie[] cookies = request.getCookies();
		
		for(int i=0; i<cookies.length; i++)
		{
			if(cookies[i].getName().startsWith("goods"))
			{
				cookies[i].setPath("/");
				cookies[i].setMaxAge(0);
				
				response.addCookie(cookies[i]); // 삭제
			}
		}
		return "redirect: list.do";
	}
	
	/////////////// cookie 끝, session 시작
	
	// 장바구니
	// session
	@GetMapping("goods/cart_list.do")
	public String goods_cart_list(int no, HttpSession session, Model model)
	{
		List<CartVO> list = (List<CartVO>)session.getAttribute("cart");
		
		model.addAttribute("list", list);
		model.addAttribute("no", no);
		
		return "goods/cart_list";
	}
	
	@PostMapping("goods/session_insert.do")
	public String goods_session_insert(int no, int account, HttpSession session, Model model)
	{
		List<CartVO> list = (List<CartVO>)session.getAttribute("cart");
		
		// session이 null 값이면
		if(list == null) 
		{
			list = new ArrayList<CartVO>(); // 새롭게 생성한다
		}
		
		// session에 저장된 데이터
		GoodsVO vo = dao.goodsDetailData(no);
		CartVO cvo = new CartVO();
		cvo.setNo(no);
		cvo.setName(vo.getGoods_name());
		cvo.setPoster(vo.getGoods_poster());
		cvo.setPrice(vo.getGoods_price());
		cvo.setAccount(account);
		
		// 장바구니 중복으로 상품 담기 방지
		boolean bCheck = false;
		for(CartVO avo:list)
		{
			if(avo.getNo() == cvo.getNo())
			{
				int acc = avo.getAccount() + cvo.getAccount();
				avo.setAccount(acc); // 같은 게 있으면 수량만 증가 시킴
				
				bCheck = true;
				break;
			}
		}
		
		if(bCheck == false) // 같은 게 없으면 있는 그대로 담음
		{
			list.add(cvo);
			session.setAttribute("cart", list);
		}
		
		return "redirect: cart_list.do?no=" + no; 
	}
	
	
	// 장바구니 부분 삭제
	@GetMapping("goods/cart_cancel.do")
	public String goods_cart_cancel(int no, HttpSession session)
	{
		List<CartVO> list = (List<CartVO>)session.getAttribute("cart");
		
		for(int i=0; i<list.size(); i++)
		{
			CartVO vo = list.get(i);
			
			if(vo.getNo() == no)
			{
				list.remove(i);	
				break;
			}
		}
		
		return "redirect: cart_list.do?no=" + no;
	}
	
	// 장바구니 전체 삭제
	@GetMapping("goods/cart_total_delete.do")
	public String cart_total_delete(int no, HttpSession session)
	{
		session.removeAttribute("cart");
		
		return "redirect: cart_list.do?no=" + no;
	}
}
