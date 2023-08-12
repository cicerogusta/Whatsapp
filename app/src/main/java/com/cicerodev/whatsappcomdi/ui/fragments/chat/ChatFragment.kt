package com.cicerodev.whatsappcomdi.ui.fragments.chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cicerodev.whatsappcomdi.R
import com.cicerodev.whatsappcomdi.adapter.MensagensAdapter
import com.cicerodev.whatsappcomdi.data.model.Mensagem
import com.cicerodev.whatsappcomdi.data.model.User
import com.cicerodev.whatsappcomdi.databinding.FragmentChatBinding
import com.cicerodev.whatsappcomdi.ui.base.BaseFragment
import com.ciceropinheiro.whatsapp_clone.util.codificarBase64


class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    private var idUsuarioRemetente: String? = null
    private var usuarioDestinatario: User? = null
    override val viewModel: ChatFragmentViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val args: ChatFragmentArgs by navArgs()
    private var listaMensagens = mutableListOf<Mensagem>()
    private lateinit var idUsuarioDestinatario: String

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatBinding = FragmentChatBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.toolbar
        toolbar.title = ""
        val appCompatActivity = (activity as AppCompatActivity?)!!
        appCompatActivity.setSupportActionBar(toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        idUsuarioRemetente = viewModel.retornaIdRemetente()

        viewModel.mensagens.value?.clear()
        observer()
        recuperaDadosUsuarioDestinatario()

        binding.content.fabEnviar.setOnClickListener {
            enviarMensagem()

        }

    }

    private fun enviarMensagem() {
        val textoMensagem = binding.content.editMensagem.text.toString()
        if (textoMensagem.isNotEmpty()) {
            if (usuarioDestinatario != null) {
                val mensagem = Mensagem()
                mensagem.idUsuario = idUsuarioRemetente
                mensagem.mensagem = textoMensagem

                viewModel.enviaMensagem(idUsuarioRemetente!!, idUsuarioDestinatario, mensagem)
                viewModel.enviaMensagem(idUsuarioDestinatario, idUsuarioRemetente!!, mensagem)
                viewModel.salvaConversa(
                    idUsuarioRemetente!!,
                    idUsuarioDestinatario,
                    usuarioDestinatario!!,
                    mensagem,
                    false,
                    null
                )
                viewModel.salvaConversa(
                    idUsuarioDestinatario,
                    idUsuarioRemetente!!,
                    viewModel.retornaUsuarioLogado(),
                    mensagem,
                    false,
                    null
                )


            } else {

                for (membro in args.grupo?.membros!!) {
                    val idRemetenteGrupo: String = codificarBase64(membro.email)
                    val idUsuarioLogadoGrupo: String = viewModel.retornaIdRemetente().toString()
                    val mensagem = Mensagem()
                    mensagem.idUsuario = (idUsuarioLogadoGrupo)
                    mensagem.mensagem = (textoMensagem)
                    mensagem.nome = (viewModel.retornaUsuarioLogado().nome)

                    //salvar mensagem para o membro
                    viewModel.enviaMensagem(idRemetenteGrupo, idUsuarioDestinatario, mensagem)

                    //Salvar conversa
                    usuarioDestinatario?.let {
                        viewModel.salvaConversa(
                            idRemetenteGrupo,
                            idUsuarioDestinatario,
                            it,
                            mensagem,
                            true, args.grupo
                        )
                    }
                }
            }

        }
    }

    private fun recuperaDadosUsuarioDestinatario() {
        if (args.tipoChat.equals("chatGrupo")) {
            val grupo = args.grupo
            if (grupo != null) {
                idUsuarioDestinatario = grupo.id
            }
            if (grupo != null) {
                binding.textViewNomeChat.setText(grupo.nome)
            }
            val foto = grupo?.foto
            if (!foto.equals("")) {
                val url = Uri.parse(foto)
                Glide.with(this).load(url).into(binding.circleImageFotoChat)
            } else {
                binding.circleImageFotoChat.setImageResource(R.drawable.padrao)
            }
        } else {
            usuarioDestinatario = args.user
            binding.textViewNomeChat.setText(usuarioDestinatario?.nome)
            val foto = usuarioDestinatario?.foto
            if (!foto.equals("")) {
                val url = Uri.parse(usuarioDestinatario?.foto ?: "")
                Glide.with(this)
                    .load(url)
                    .into(binding.circleImageFotoChat)

            } else {
                binding.circleImageFotoChat.setImageResource(R.drawable.padrao)
            }


            //recuperar dados usuario destinatario
            idUsuarioDestinatario = usuarioDestinatario?.email?.let { codificarBase64(it) }.toString()
        }
    }

    private fun observer() {
        viewModel.mensagens.observe(viewLifecycleOwner) {
            it.forEach { mensagem ->
                listaMensagens.add(mensagem)
            }
            configuraRecyclerView()
        }
    }


    private fun configuraRecyclerView() {
        binding.content.recyclerMensagens.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter =
                MensagensAdapter(listaMensagens, requireContext(), viewModel.retornaIdRemetente()!!)
            this.adapter?.notifyDataSetChanged()

        }
    }

    override fun onStart() {
        super.onStart()
        configuraRecyclerView()
        viewModel.retornaMensagem(idUsuarioRemetente.toString(), idUsuarioDestinatario)


    }

    override fun onStop() {
        super.onStop()
        viewModel.mensagens.value?.clear()
    }
}